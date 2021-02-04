## Signal - Short description of plugins

### Signal opener
- opens signals or plots
- comma delimited text files
- columns MUST have the same number of rows
- missing values MUST be filled up with  "NaN"
- NaNs will be ignored for plot charts
- NaNs will be ignored by signal plugins

### Fractal dimension - Higuchi
- computes Higuchi dimensions of signals.
- Signals should be opened with the CSAJ Signal Opener.
- comma delimited text files
- columns MUST have the same number of rows
- missing values MUST be filled up with  "NaN"
- NaNs will be ignored.
- Ref.: Higuchi, Physica D, 1988, DOI 10.1016/0167-2789(88)90081-4

### Dectect QRS peaks
- detects QRS peaks and peak to peak intervals
- based on the OSEA algorithm
- for ecg files (Holter chan.raw file).
- Signals should be opened with the CSAJ Signal Opener.
- comma delimited text files
- columns MUST have the same number of rows
- missing values MUST be filled up with  "NaN"
- NaNs will be ignored.
- the osea-4-java algorithm/library developed by Patrick S. Hamilton from EP Limited is implemented
- see https://github.com/MEDEVIT/OSEA-4-Java
- Ref.: Hamilton, Tompkins, W. J., "Quantitative investigation of QRS detection rules using the MIT/BIH arrhythmia database", IEEE Trans. Biomed. Eng., BME-33, pp. 1158-1165, 1987.
