package springmvcframework;

import com.ljt.demo.service.impl.UserServiceImpl;

import annotation.MyService;
/**
 * 
 * 基于反射读取注解值测试
 *
 */
public class MyServiceTest {
	public static void main(String[] args) {
		UserServiceImpl us = new UserServiceImpl();
		Class<? extends UserServiceImpl> clazz = us.getClass();
		if(clazz.isAnnotationPresent(MyService.class)) {
			MyService annotation = clazz.getAnnotation(MyService.class);
			String value = annotation.value();
			System.out.println("value:" + value);
		}
	}
}
