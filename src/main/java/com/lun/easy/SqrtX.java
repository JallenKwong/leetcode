package com.lun.easy;

public class SqrtX {
	public int mySqrt(int x) {

		int left = 1, right = x;

		while (left <= right) {
			int mid = left + (right - left) / 2;
			if (mid == x / mid) {
				return mid;
			} else if (mid < x / mid) {
				left = mid + 1;
			} else {
				right = mid - 1;
			}
		}

		return right;
	}

	public int mySqrt2(int x) {
		if (x == 0)
			return 0;
		double last = 0;
		double res = 1;
		while (res != last) {
			last = res;
			res = (res + x / res) / 2;
		}
		return (int) res;

	}
}