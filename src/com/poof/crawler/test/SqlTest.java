package com.poof.crawler.test;

import java.util.ArrayList;
import java.util.List;

public class SqlTest {
	public static void main(String[] args) {
		List<Integer> q = new ArrayList<Integer>();
		for (int i = 0; i < 5; i++) {
			q.add(i);
		}
		

		int size = q.size() / 20;
		size = q.size() % 20 >= 0 ? size + 1 : size; // 5521,5
		for (int i = 0; i < size; i++) { // 6
			for (int j = 0; j < (i == size - 1 ? q.size() % 20 : 20); j++) {
				Integer c = q.get(i * 20 + j);
				System.err.println(c);
			}

		}
	}
}
