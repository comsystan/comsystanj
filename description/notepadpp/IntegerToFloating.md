### Change integer to floating numbers in a csv table with Notepad++

- Open the csv text file with Notepad++
- Go to Search/Replace or press ctrl-H
- Click Wrap around
- Set Search Mode to Regular expression
- Search for: (^|,)([0-9]+)(,|\r)
- Replace with: \1\2.0\3
- If it does not work for the last column try \n or \r\n instead of \r
![Image](IntegerToFloating.png)
