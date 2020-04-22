package springmvcframework.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import annotation.MyAutowired;
import annotation.MyController;
import annotation.MyRequestMapping;
import annotation.MyService;
/*
 * 1、Spring中的bean是线程安全的吗？
 * bean是存在IOC容器中的，是通过反射的方式创建出来的，bean是使用者自己写的，bean是否是线程安全是由使用者决定的，spring只是将bean存起来了
 * 2、Spring中的Bean是如何被回收的？
 * 对象为引用不可达，会被GC回收。
 * bean存在用IOC容器中，就是map集合，只有当map中的值变为Null，或者没被任何地方引用时候才会被回收。
 * IOC容器是全局的，随Spring的启动而启动，随Spring的消亡而消亡
 * 结论：
 * 当bean为单列时全程都不可能被回收，为多例时使用完就等待GC回收。
 */
/**
 * 单列模式
 * 工厂模式
 * 模板模式
 * 策略模式
 * 委派模式
 * 装饰器模式
 * 适配器模式
 * 观察者模式
 * 享元模式
 * 原型模式
 * @author 11404
 *
 */
public class MyDispatcherServlet extends HttpServlet{//模板模式

	private static final long serialVersionUID = 8684700010110115341L;
	
	private Properties contextConfig = new Properties();

	//保存扫描包下所有带有（MyService,MyController注解）类的类名
	private List<String> classNames = new ArrayList<String>();
	
	private Map<String,Object> ioc = new ConcurrentHashMap<String,Object>();
	
	private Map<String,Method> handlerMapping = new ConcurrentHashMap<String,Method>();
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		//6、调度、分发
		try {
			doDispatch(req, resp);
		} catch (Exception e) {
			e.printStackTrace();
			resp.getWriter().write("500 !" + e.getMessage());
		}
	}
	// 委派模式
	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		String url = req.getRequestURI();
		String contextPath = req.getContextPath();
		url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
		
		if(!this.handlerMapping.containsKey(url)) {
			resp.getWriter().write("404 Not Found !");
			return;
		}
		
		Method method = this.handlerMapping.get(url);
		Map<String, String[]> params = req.getParameterMap();
		//获取方法所在类的类名
		String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
		method.invoke(ioc.get(beanName), new Object[] {req,resp,params.get("name")[0]});
		
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		System.out.println("MyDispatcherServlet init ....");
		//1、加载配置文件
		doLoadConfig(config.getInitParameter("contextConfigLocation"));
		
		//2、解析配置文件，扫描相关类
		doScanner(contextConfig.getProperty("scanPackage"));
		
		//3、初始化相关类，并放入IOC容器中
		doInstance();
		
		//4、完成依赖注入
		doAutowired();
		
		//5、初始化HandlerMapping映射
		initHandlerMapping();
		
		System.out.println("MySpring init completed");
		
	}

	//策略模式（一个url对应一个method）
	private void initHandlerMapping() {
		if(ioc.isEmpty()) {return;}
		for (Entry<String, Object> entry : ioc.entrySet()) {
			Class<? extends Object> clazz = entry.getValue().getClass();
			if(!clazz.isAnnotationPresent(MyController.class)) {continue;}
				String baseUrl = "";
				MyController controller = clazz.getAnnotation(MyController.class);
				baseUrl = controller.value();
				for (Method method : clazz.getMethods()) {
					if(!method.isAnnotationPresent(MyRequestMapping.class)) {continue;}
					MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
					// /demo/query
					// demo query
					String url = ("/" + baseUrl + requestMapping.value()).replaceAll("/+", "/");
					handlerMapping.put(url, method);
					
					System.out.println("Mapped:" + url + ", " + method);
				}
		}
	}

	private void doAutowired() {
		if(ioc.isEmpty()) {return;}
		for (Entry<String, Object> entry : ioc.entrySet()) {
			Field[] fields = entry.getValue().getClass().getDeclaredFields();
			for (Field field : fields) {
				if(!field.isAnnotationPresent(MyAutowired.class)) {continue;}
				MyAutowired autowired = field.getAnnotation(MyAutowired.class);
				String beanName = autowired.value().trim();//注解声明了beanName用声明的
				if("".equals(beanName)) {//注解没有声明值则按照接口名称
					beanName = field.getType().getName();
				}
				//对private protected修饰的成员变量暴力访问
				field.setAccessible(true);
				//对成员对象属性赋值
				try {
					field.set(entry.getValue(), ioc.get(beanName));
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	//工厂模式
	private void doInstance() {
		if(classNames.isEmpty()) {return;}
		for (String className : classNames) {
			try {
				Class<?> clazz = Class.forName(className);
				// 有MyController MyService注解的类才实例化
				if(clazz.isAnnotationPresent(MyController.class)) {
					Object instance = clazz.newInstance();
					String beanName = toLowerFirstCase(clazz.getSimpleName());
					ioc.put(beanName, instance);
				}else if(clazz.isAnnotationPresent(MyService.class)) {
					//1、默认类名首字母小写作为beanName
					String beanName = toLowerFirstCase(clazz.getSimpleName());
					//2、自定义的beanName
					MyService service = clazz.getAnnotation(MyService.class);
					if(!"".equals(service.value())) {
						beanName = service.value();
					}
					Object instance = clazz.newInstance();
					ioc.put(beanName, instance);
					//3、创建接口的实例
					for (Class<?> i : clazz.getInterfaces()) {
						//单列模式
						if(ioc.containsKey(i.getName())) {// 对于beanName重名的抛出异常
							throw new Exception("The beanName["+i.getName()+"] is exists!");
						}
						ioc.put(i.getName(), instance);
					}
					
				}else {
					continue;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 将字符串首字母改为小写
	 * @param simpleName
	 * @return
	 */
	private String toLowerFirstCase(String simpleName) {
		char[] chars = simpleName.toCharArray();
		chars[0] += 32;
		return String.valueOf(chars);
	}

	private void doScanner(String scanPackage) {
		//获取配置扫描路径下的所有文件
		URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
		File classPath = new File(url.getFile());
		for(File file : classPath.listFiles()) {
			if(file.isDirectory()) {//子包下.class文件递归处理
				doScanner(scanPackage + "." + file.getName());
			}
			if(!file.getName().endsWith(".class")) {
				continue;
			}
			//直接文件夹下.class文件处理
			String className = scanPackage + "." + file.getName().replace(".class", "");
			classNames.add(className);
		}
	}

	private void doLoadConfig(String contextConfigLocation) {
		URL resource = this.getClass().getClassLoader().getResource("/");
		System.out.println(resource.getPath());
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
		try {
			contextConfig.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			if(null != is) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
