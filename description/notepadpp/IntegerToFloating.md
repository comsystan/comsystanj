### Change integer to floating numbers in a csv table with Notepad++

- Open the csv text file with Notepad++
- Go to Search/Replace or press Ctrl-H
- Click Wrap around
- Set Search Mode to Regular expression
- Search and replace **two times**:
  - <p>Search for: ,([0-9]+)(,|\r)</p>
  - <p>Replace with: ,\1.0\2</p>
- Check the result
- If it does not work for the last column, try \n or \r\n instead of \r
![Notepadpp-IntegerToFloating.png](Notepadpp-IntegerToFloating.png)
