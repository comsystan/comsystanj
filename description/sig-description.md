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
- Filling up missing values by NaNs with Notepad++ can be found [here](notepadpp/FillNaNs.md) 

### Detect QRS peaks
- Detects QRS peaks and peak to peak intervals of ECG data
- Based on the OSEA algorithm
- Signals should be opened with the CSAJ Signal Opener
- For ecg files (Holter chan.raw file)
- The osea-4-java algorithm/library developed by Patrick S. Hamilton from EP Limited is implemented
- See [https://github.com/MEDEVIT/OSEA-4-Java](https://github.com/MEDEVIT/OSEA-4-Java)
- Ref.: Hamilton & Tompkins, 1987, IEEE Trans.Biomed.Eng., [https://doi.org/10.1109/TBME.1986.325695](https://doi.org/10.1109/TBME.1986.325695)

### Detrended fluctuation analysis
- Computes Detrended fluctuation analysis.
- Signals should be opened with the CSAJ Signal Opener
- Evaluation of Entire signal or Subsequent/Gliding boxes
- Note for Entire signal:
  - Maximal window size should not be larger than 1/3 of the signal length
- Notes for Subsequent/Gliding box:
  - Maximal window size should not be larger than 1/3 of the box size
  - the box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Peng et al., 1994, Phys.Rev.E., [https://doi.org/10.1103/physreve.49.1685](https://doi.org/10.1103/physreve.49.1685)

### Fractal dimension - Higuchi
- Computes Higuchi dimensions of signals.
- Signals should be opened with the CSAJ Signal Opener
- Evaluation of Entire signal or Subsequent/Gliding boxes
- Note for Entire signal:
  - kMax should not be larger than 1/3 of the signal length
- Notes for Subsequent/Gliding box:
  - kMax should not be larger than 1/3 of the box size
  - the box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Higuchi, 1988, Physica D, [https://doi.org/10.1016/0167-2789(88)90081-4](https://doi.org/10.1016/0167-2789(88)90081-4)

### Kolmogorov complexity and Logical Depth
- KC is estiamted by compressing data bytes (ZLIB, GZIB)
- LD is estimated by the decompression time
- Iterations should be set to as high a value as possible
- LD values should be taken with caution, as computers are not well suited to measure times
- Ref.: Zenil etal., Complexity, 2012, [https://doi.org/10.1002/cplx.20388](https://doi.org/10.1002/cplx.20388)

### Sample entropy
- Computes Sample or Approximate entropies of signals
- Signals should be opened with the CSAJ Signal Opener
- Note: Approximate entropy is not recommended for different signal lengths
- m length of subsignals
- r maximal distance radius (0.1SD < r < 0.25SD, with SD the standard deviation of the time series)
- d additional delay according to Govindan et.al., 2007, PhysicaA, [https://doi.org/10.1016/j.physa.2006.10.077](https://doi.org/10.1016/j.physa.2006.10.077)
- Evaluation of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - kMax should not be larger than 1/3 of the box size
  - the box size should not be larger than 1/3 of the signal length 
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - The number of subsequent and particularly of gliding boxes can be very high
- Ref.: Richman & Moorman, 2000,Am.J.Physiol.Heart.Circ.Physiol., [https://doi.org/10.1152/ajpheart.2000.278.6.H2039](https://doi.org/10.1152/ajpheart.2000.278.6.H2039)

### Signal statistics
- Computes descriptive statistics of signals.
- Signals should be opened with the CSAJ Signal Opener
- Evaluation of Entire signal or Subsequent/Gliding boxes
- Notes for Subsequent/Gliding box:
  - Only Medians, Means and SDs are computed
  - The number of subsequent boxes is (signal length)/(box size)
  - The number of gliding boxes is (signal length)-(box size)
  - The number of subsequent and particularly of gliding boxes can be very high
