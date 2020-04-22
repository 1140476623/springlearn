# springlearn
简单实现spring + springmvc
核心类为springmvcframework.servlet.MyDispatcherServlet

思路：
1、配置
  配置web.xml     MyDispatcherServlet
  设定init-param  contextConfigLocation=classpath:application.properties
  设定url-pattern /*
  配置Annotation  @MyController @MyService  @MyAutowired  ...
 2、初始化
 调用init()方法   加载配置文件
 IOC容器初始化     Map<String,Object>
 扫描相关的类     scan-package="com.ljt.demo"
 创建实例化并保存至容器(IOC)  通过反射机制将类实例化放入IOC容器
 进行DI操作(DI)   扫描IOC容器中的实例，给没有赋值的属性自动赋值
 初始化HandlerMapping(MVC)  将一个URL和一个Method进行一对一的关联映射Map<String,Method>
 3、运行阶段
 调用doPost()/doGet()   web容器调用doPost()/doGet()方法，获得request/response对象
 匹配HandlerMapping     从request对象中获得用户输入的url，找到对应的Method
 反射调用method.invoker() 利用反射调用方法并返回结果
 response.getWrite().write()  将返回结果输出到浏览器
 
