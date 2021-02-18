### How to fill up a csv table with NaNs using Notepad++

- Open the csv text file with Notepad++
- Go to Search/Replace or press ctrl-H
- Click Wrap around
- Set Search Mode to Regular expression
  1. For the first column --> replace without brackets:  (^,) with    (NaN,)
  2. For inbetween NaNs --> replace without brackets: (,,) with (,NaN,)  
  3. Repeat step 2 --> replace without brackets: (,,) with (,NaN,)
  4. For the last column --> replace without brackets :  (,\r)   with (,NaN\r)
- If step 4 does not work try (\n) or (\r\n)  instead of (\r)
