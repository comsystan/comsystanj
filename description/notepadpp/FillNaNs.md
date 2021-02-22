### Filling up NaNs in a csv table with Notepad++

- Open the csv text file with Notepad++
- Go to Search/Replace or press Ctrl-H
- Click Wrap around
- Set Search Mode to Regular expression
- Run two times through the table
  - <p> Search for: (^|,)(,|\r)</p>
  - <p> Replace with: \1NaN\2</p>
- If it does not work for the last column, try \n or \r\n instead of \r
![Notepadpp-FillNaNs](Notepadpp-FillNaNs.png)
