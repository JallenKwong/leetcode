package com.lun.util;


public class BinaryTree {

	/**
	 * 二叉树结点
	 * 
	 * @author 白居布衣
	 *
	 */
	public static class TreeNode {
		public int val;
		public TreeNode left;
		public TreeNode right;
		public TreeNode(int x) { val = x; }

	}
	
	
	/**
	 * 非递归版二叉查找树的构建
	 * 
	 * @param array
	 * @return
	 */
	public static TreeNode integerArray2BinaryTree(int[] array) {
		if(array == null || array.length == 0) {
			throw new IllegalArgumentException("Illegal array.");
		}
		TreeNode root = new TreeNode(array[0]);
		for(int i = 1; i < array.length; i++) {
			TreeNode p = root;
			while(true) {
				if(array[i] < p.val) {
					if(p.left != null) {
						p = p.left;
					}else {
						p.left = new TreeNode(array[i]);
						break;
					}
				}
				
				if(array[i] > p.val){
					if(p.right != null) {
						p = p.right;
					}else {
						p.right = new TreeNode(array[i]);
						break;
					}
				}
				
				if(array[i] == p.val) 
					break;
			}
		}
		return root;
	}
	
	/**
	 * 前序遍历
	 * 
	 * @param root
	 */
	public static void preorderTraversing(TreeNode root) {
		if(root == null)
			return;
		System.out.print(root.val + " ");
		inorderTraversing(root.left);
		inorderTraversing(root.right);
	}
	
	
	
	/**
	 * 中序遍历打印
	 * 
	 * @param root
	 */
	public static void inorderTraversing(TreeNode root) {
		if(root == null)
			return;
		inorderTraversing(root.left);
		System.out.print(root.val + " ");
		inorderTraversing(root.right);
	}
	
	/**
	 * 后序遍历打印
	 * 
	 * @param root
	 */
	public static void postTraversing(TreeNode root) {
		if(root == null)
			return;
		inorderTraversing(root.left);
		inorderTraversing(root.right);
		System.out.print(root.val + " ");
	}
	
	/**
	 * 找出值最小结点
	 * 
	 * @param p
	 * @return
	 */
	public static TreeNode min(TreeNode p) {
		if(p == null) {
			//throw new IllegalArgumentException("Illegal TreeNode.");
			return null;
		}
		
		if(p.left == null)
    		return p;
    	return min(p.left);
    }
	
	/**
	 * 找出值最大结点
	 * 
	 * @param p
	 * @return
	 */
	public static TreeNode max(TreeNode p) {
		if(p == null) {
			//throw new IllegalArgumentException("Illegal TreeNode.");
			return null;
		}
		
		if(p.right == null)
    		return p;
    	return max(p.right);
    }
	
}