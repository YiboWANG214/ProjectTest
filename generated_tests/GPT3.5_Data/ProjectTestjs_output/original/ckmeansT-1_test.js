// Unit test for numeric_sort.js

import numericSort from './numeric_sort.js';

describe('numericSort', () => {
    test('should sort an array of numbers in ascending order', () => {
        const input = [3, 2, 1];
        const expected = [1, 2, 3];
        expect(numericSort(input)).toEqual(expected);
    });

    test('should not change the original array', () => {
        const input = [3, 2, 1];
        const sorted = numericSort(input);
        expect(input).not.toEqual(sorted);
    });
});

// Unit test for unique_count_sorted.js

import uniqueCountSorted from './unique_count_sorted.js';

describe('uniqueCountSorted', () => {
    test('should return the count of unique values in a sorted array', () => {
        const input = [1, 2, 3];
        const expected = 3;
        expect(uniqueCountSorted(input)).toBe(expected);
    });

    test('should handle arrays with duplicate values', () => {
        const input = [1, 1, 1];
        const expected = 1;
        expect(uniqueCountSorted(input)).toBe(expected);
    });
});

// Unit test for make_matrix.js

import makeMatrix from './make_matrix.js';

describe('makeMatrix', () => {
    test('should create a matrix with the specified number of rows and columns', () => {
        const columns = 3;
        const rows = 2;
        const expected = [
            [0, 0],
            [0, 0],
            [0, 0]
        ];
        expect(makeMatrix(columns, rows)).toEqual(expected);
    });

    test('should create a matrix with all elements initialized to 0', () => {
        const columns = 2;
        const rows = 2;
        const matrix = makeMatrix(columns, rows);
        matrix.forEach(column => {
            column.forEach(element => {
                expect(element).toBe(0);
            });
        });
    });
});

// Unit test for ckmeans.js

import ckmeans from './ckmeans.js';

describe('ckmeans', () => {
    test('should separate input values into clusters based on desired number of clusters', () => {
        const input = [-1, 2, -1, 2, 4, 5, 6, -1, 2, -1];
        const nClusters = 3;
        const expected = [[-1, -1, -1, -1], [2, 2, 2], [4, 5, 6]];
        expect(ckmeans(input, nClusters)).toEqual(expected);
    });

    test('should throw an error if the number of requested clusters is greater than the input size', () => {
        const input = [1, 2, 3];
        const nClusters = 4;
        const errorMessage = 'cannot generate more classes than there are data values';
        expect(() => ckmeans(input, nClusters)).toThrowError(errorMessage);
    });
});
