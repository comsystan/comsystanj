## Signal - Short description of plugins

### Signal opener
- Opens signals or plots of floating values (dot decimal numbers)
- For comma delimited text files
- The first row must contain the column (signal) headers
- The first column must contain the row headers or just a subsequent numbering 
- All columns MUST have the same number of rows
- Missing values MUST be filled up with NaN
- NaNs will be ignored for plot charts
- NaNs will be ignored by CSAJ signal plugins
- Conversion of integer to floating numbers with Notepad++ can be found [here](notepadpp/IntegerToFloating.md) 
- Filling up NaNs with Notepad++ can be found [here](notepadpp/FillNaNs.md) 

### Signal statistics
- Computes descriptive statistics of signals.
- Signals should be opened with the CSAJ Signal Opener
- Evaluation of entire signal or subsequent/gliding boxes

### Fractal dimension - Higuchi
- Computes Higuchi dimensions of signals.
- Signals should be opened with the CSAJ Signal Opener
- Evaluation of entire signal or subsequent/gliding boxes
- Ref.: Higuchi, Physica D, 1988, DOI 10.1016/0167-2789(88)90081-4

### Dectect QRS peaks
- Detects QRS peaks and peak to peak intervals
- Based on the OSEA algorithm
- Signals should be opened with the CSAJ Signal Opener
- For ecg files (Holter chan.raw file)
- The osea-4-java algorithm/library developed by Patrick S. Hamilton from EP Limited is implemented
- See https://github.com/MEDEVIT/OSEA-4-Java
- Ref.: Hamilton, Tompkins, W. J., "Quantitative investigation of QRS detection rules using the MIT/BIH arrhythmia database", IEEE Trans. Biomed. Eng., BME-33, pp. 1158-1165, 1987.
