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
- Check the last row of the last column, because there is usually no Carriage Return
![Notepadpp-FillNaNs](Notepadpp-FillNaNs.png)
- For very large tables this search method may not be successfull
- Then you should try following subsequent search and replace steps
- For the first column (can be skipped, if the row numbering is complete):
  - Search for: ^,
  - Replace with: NaN,
- For the body of the table **two times**:
  - Search for: ,,
  - Replace with: ,NaN,
- For the last column:
  - Search for: ,\r
  - Replace with: ,NaN\r
- If it does not work for the last column, try \n or \r\n instead of \r
- Check the last row of the last column, because there is usually no Carriage Return
