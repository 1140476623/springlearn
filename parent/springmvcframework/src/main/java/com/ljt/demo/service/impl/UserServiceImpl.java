package com.ljt.demo.service.impl;

import com.ljt.demo.service.UserService;

import annotation.MyService;

@MyService("userServiceImpl")
public class UserServiceImpl implements UserService{

	@Override
	public String getName(String name) {
		return "My name is " + name +", from UserService !";
	}
	
}
