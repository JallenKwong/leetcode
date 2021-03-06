package com.lun.swordtowardoffer;

import com.lun.util.SinglyLinkedList.ListNode;

public class FindKthToTail {
	
	/**
	 * 递归的方式
	 * 
	 */
	public ListNode find(ListNode head, int k) {
		if(head == null || k <= 0) 
			throw new IllegalArgumentException();
		return find(head, k, new int[] {0});
	}
	
	private ListNode find(ListNode head, int k, int[] count) {
		if(head == null) {
			return null;
		}
		
		ListNode node = find(head.next, k, count);
		count[0] += 1;
		
		if(count[0] == k) {
			return head;
		}
		
		return node;
	}
	
	
	/**
	 * 我亲手撸的
	 * 用两指针的方式
	 * 
	 */
	public ListNode find2(ListNode head, int k) {
		
		if(head == null || k <= 0) 
			throw new IllegalArgumentException();
		
		ListNode p1 = null, p2 = head;
		
		int count = 1;
		
		while(p2.next != null) {
			count++;
			p2 = p2.next;
			if(count - k == 1) {
				p1 = head;
				break;
			}
		}
		
		if(count == k) {
			return head;
		}else if (count < k) {
			return null;
		}else {
			while(p2 != null) {
				p1 = p1.next;
				p2 = p2.next;
			}
		}
		
		return p1;
	}
	
	/**
	 * 书上的
	 * 用两指针的方式
	 * 
	 */
	public ListNode find3(ListNode head, int k) {
		if(head == null || k <= 0) 
			throw new IllegalArgumentException();
		
		ListNode p1 = head;
		ListNode p2 = head;
		
		for(int i = 0; i < k - 1; i++) {
			if(p2 == null) return null;
			p2 = p2.next;
		}
		
		if(p2 == null)
			return null;
		
		while(p2.next != null) {
			p1 = p1.next;
			p2 = p2.next;
		}
		
		return p1;
	}
	
}
