# HAM-Radio-transceiver-DA

**table of content**
- [HAM-Radio-transceiver-DA](#ham-radio-transceiver-da)
  - [Hardware](#hardware)
    - [Transceiver basic setup](#transceiver-basic-setup)
      - [Receiver basic setup](#receiver-basic-setup)
      - [Transmitter basic setup](#transmitter-basic-setup)
---
## Hardware

### Transceiver basic setup
Source: [land-boards.com](https://land-boards.com/blwiki/index.php?title=File:FARHADPNG.PNG)
<img src="doc/img/basic/FARHADPNG (1).png" width=100%> <br>

#### Receiver basic setup
The receiving part of the radio works as follows.

1. The signals get picked up by the antenna 
2. The picked up signals get filtered to select the right band we want to receive. 
3. As the signals are at -80 to -110dB at this point we need to amplify the signal using a Termination Insensitive Amplifier.
4. To hear the right station we need to mix the signal with a variable frequency oscillator to bring the station you want to hear in the passband of the IF-Filter.
5. Amplify the signal 
6. The signal goes through the SSB-Filter designed for 16MHz and 2.7kHz of bandwidth. 
7. Amplify the signal 
8. Demodulate the signal using a ring mixer. 
9. Amplify the signal for the headphones. 


#### Transmitter basic setup

