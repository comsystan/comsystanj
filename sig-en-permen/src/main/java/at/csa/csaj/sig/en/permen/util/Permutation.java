/*-
 * #%L
 * Project: ImageJ signal plugin for computing the Permutation entropy
 * File: Permutation.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2021 Comsystan Software
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package at.csa.csaj.sig.en.permen.util;

public class Permutation {
	public static void main(String[] args) {
		printArray(allPermutations(8));
	}

	public static int[][] allPermutations(int N) {
		// base case
		if (N == 2) {
			return new int[][] { { 1, 2 }, { 2, 1 } };
		} else if (N > 2) {
			// start with all permutations of previous degree
			int[][] permutations = allPermutations(N - 1);

			for (int i = 0; i < factorial(N); i += N) {
				// copy each permutation N - 1 times
				for (int j = 0; j < N - 1; ++j) {
					// similar to javascript's array.splice
					permutations = insertRow(permutations, i, permutations[i]);
				}
			}

			// "weave" next number in
			for (int i = 0, j = N - 1, d = -1; i < permutations.length; ++i) {
				// insert number N at index j
				// similar to javascript's array.splice
				permutations = insertColumn(permutations, i, j, N);

				// index j is N-1, N-2, N-3, ... , 1, 0; then 0, 1, 2, ... N-1;
				// then N-1, N-2, etc.
				j += d;

				// at beginning or end of the row, switch weave direction
				if (j < 0 || j > N - 1) {
					d *= -1;
					j += d;
				}
			}

			return permutations;
		} else {
			throw new IllegalArgumentException("N must be >= 2");
		}
	}

	private static void arrayDeepCopy(int[][] src, int srcRow, int[][] dest,
			int destRow, int numOfRows) {
		for (int row = 0; row < numOfRows; ++row) {
			System.arraycopy(src[srcRow + row], 0, dest[destRow + row], 0,
					src[row].length);
		}
	}

	public static int factorial(int n) {
		return n == 1 ? 1 : n * factorial(n - 1);
	}

	private static int[][] insertColumn(int[][] src, int rowIndex,
			int columnIndex, int columnValue) {
		int[][] dest = new int[src.length][0];

		for (int i = 0; i < dest.length; ++i) {
			dest[i] = new int[src[i].length];
		}

		arrayDeepCopy(src, 0, dest, 0, src.length);

		int numOfColumns = src[rowIndex].length;

		int[] rowWithExtraColumn = new int[numOfColumns + 1];

		System.arraycopy(src[rowIndex], 0, rowWithExtraColumn, 0, columnIndex);

		System.arraycopy(src[rowIndex], columnIndex, rowWithExtraColumn,
				columnIndex + 1, numOfColumns - columnIndex);

		rowWithExtraColumn[columnIndex] = columnValue;

		dest[rowIndex] = rowWithExtraColumn;

		return dest;
	}

	private static int[][] insertRow(int[][] src, int rowIndex,
			int[] rowElements) {
		int srcRows = src.length;
		int srcCols = rowElements.length;

		int[][] dest = new int[srcRows + 1][srcCols];

		arrayDeepCopy(src, 0, dest, 0, rowIndex);
		arrayDeepCopy(src, rowIndex, dest, rowIndex + 1, src.length - rowIndex);

		System.arraycopy(rowElements, 0, dest[rowIndex], 0, rowElements.length);

		return dest;
	}

	public static void printArray(int[][] array) {
		for (int row = 0; row < array.length; ++row) {
			for (int col = 0; col < array[row].length; ++col) {
				System.out.print(array[row][col] + " ");
			}

			System.out.print("\n");
		}

		System.out.print("\n");
	}
}
