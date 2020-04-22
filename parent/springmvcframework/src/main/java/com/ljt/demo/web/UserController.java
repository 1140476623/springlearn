package com.ljt.demo.web;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ljt.demo.service.UserService;

import annotation.MyAutowired;
import annotation.MyController;
import annotation.MyRequestMapping;
import annotation.MyRequestParam;

@MyController("/demo")
public class UserController {
	
	@MyAutowired
	private UserService userService;
	
	@MyRequestMapping("/test")
	public void demo(HttpServletRequest req, HttpServletResponse resp, @MyRequestParam String name) {
		String msg = userService.getName(name);
		try {
			resp.getWriter().write(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
