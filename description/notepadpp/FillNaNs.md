### Filling up NaNs in a csv table with Notepad++

- Open the csv text file with Notepad++
- Go to Search/Replace or press Ctrl-H
- Click Wrap around
- Set Search Mode to Regular expression
- Search and replace **two times**: 
  - <p>Search for: (^|,)(,|\r)</p>
  - <p>Replace with: \1NaN\2</p>
- Check the result
- If it does not work for the last column, try \n or \r\n instead of \r
- Check the last row of the last column, because thre is usually no Carriage Return
![Notepadpp-FillNaNs](Notepadpp-FillNaNs.png)
- For very large tables this search method may not be successfull
- Then you should try following subsequent replace steps:
- <p>Search and replace missing values in the first column (row headers):
Note: This step can be skipped, when the row numbering is complete
  - Search for: ^,
  - Replace with: NaN,</p>
- <p>Search and replace missing values in the body of the table **two times**:
  - Search for: ,,
  - Replace with: ,NaN,</p>
- <p>Search and replace missing values in the last column:
  - Search for: ,\r
  - Replace with: ,NaN\r</p>
- If it does not work for the last column, try \n or \r\n instead of \r
- Check the last row of the last column, because thre is usually no Carriage Return
