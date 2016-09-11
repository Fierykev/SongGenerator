package karma;

import java.util.ArrayList;

import javax.sound.sampled.AudioInputStream;

public class BeatDetection extends BeatMarkov {

	// the amount of the song that will be looked at

	final static double SONG_SCALE = 1.0;

	// smooth the song out

	final static int SMOOTH = 10;

	// used to calculate how samples are split up

	final static int LINEAR_INCA = 0;

	final static int LINEAR_INCB = 32;

	// the number of checks to determine if a beat exists

	final static int NUM_CHECKS = 2;

	// the lowest possible band for something to be considered a beat

	final static int LOWEST_BAND = 30;

	// the number of bands on the sound spectrum

	final static int NUM_BANDS = 1000;

	double subbandToFrequency = 0;

	ArrayList<Beat> beats = new ArrayList<Beat>();

	/**
	 * Analyze the wave file and find its beats.
	 */

	public void analyzeWave(AudioInputStream audioInputStream,
			byte[] waveData) {
		final double byteSize = waveData.length; // length of the byte array
													// from the wave file

		final int sampleSize = audioInputStream.getFormat()
				.getSampleSizeInBits() * 64; // the sample size (usually 1024)

		// the size of the data after being converted to bytes

		final int datasize = (int) (Math
				.ceil((byteSize / 2.0 * SONG_SCALE) / sampleSize) * sampleSize);

		Complex datac[] = new Complex[sampleSize];

		Complex fftTransfer[];

		Complex datac2[] = new Complex[datasize];

		for (int l = 0; l < datasize / sampleSize; l++) {

			for (int i = 0; i < sampleSize; i++) {
				if ((l * sampleSize + i) * 2 < byteSize)
					datac[i] = new Complex(
							(((int) (waveData[(l * sampleSize + i) * 2
									+ 1] << 8)
									| waveData[(l * sampleSize + i) * 2] & 0xFF)
									/ SMOOTH),
							0); // convert the data out of
								// BigEndian form
				else
					datac[i] = new Complex(0, 0);
			}

			// run the fft

			fftTransfer = FFT.fft(datac);

			// copy the data to the larger set of data

			for (int i = 0; i < sampleSize; i++)
				datac2[l * sampleSize + i] = new Complex(fftTransfer[i].re(),
						fftTransfer[i].im());
		}

		double B[] = new double[datasize]; // create the double to store the
											// amps

		// find the amps

		for (int i = 0; i < datasize; i++) {
			// mod(x + iy) = sqrt(x^2 + y^2)

			B[i] = Math.sqrt((datac2[i].re() * datac2[i].re())
					+ (datac2[i].im() * datac2[i].im()));

		}

		// calculate the number of subbands

		int sum = sampleSize;

		int subbands;

		for (subbands = 1; 0 < sum; subbands++)
			sum -= (int) (LINEAR_INCA * subbands + LINEAR_INCB);

		subbands--; // correct for the extra subband

		double Es[] = new double[datasize / sampleSize * subbands];

		int wi; // width of the subband

		// analyze for each sample

		for (int l = 0; l < datasize / sampleSize; l++) {
			sum = 0;

			for (int i = 0; i < subbands; i++) // divide into subbands
			{
				wi = (int) (i < subbands - 1
						? LINEAR_INCA * (i + 1) + LINEAR_INCB
						: sampleSize - sum); // width of the subband

				Es[l * subbands + i] = 0; // clear the value

				for (int k = sum; k < sum + wi; k++)
					Es[l * subbands + i] += B[l * sampleSize + k];

				sum += wi; // add the width to the sum

				Es[l * subbands + i] *= (double) wi / (double) sampleSize;
			}
		}

		// average subband energy

		double Ei[] = new double[datasize / sampleSize * subbands];

		double EiSum = 0;

		for (int i = 0; i < 43; i++)
			EiSum += Es[i];

		Ei[0] = EiSum / 43.0;

		double maxEi = Ei[0]; // find the highest Ei value for lanes

		double minEi = Ei[0]; // find the lowest Ei value for lanes

		for (int i = 1; i < datasize / sampleSize * subbands - 43; i++) {
			// energy calc

			EiSum += Es[i + 42] - Es[i - 1];

			Ei[i] = EiSum / 43.0;

			if (maxEi < Ei[i])
				maxEi = Ei[i];

			if (Ei[i] < minEi)
				minEi = Ei[i];
		}

		// find the size of each band for the map

		double bandsize = (maxEi - minEi) / NUM_BANDS;

		int bandnum;

		double max = 0;

		// store the value that convert frequencies to subbands

		subbandToFrequency = bandsize + minEi;

		for (int l = 42; l + sampleSize < datasize / sampleSize
				* subbands; l += sampleSize) {
			max = 0;

			for (int j = 0; j < sampleSize; j += NUM_CHECKS) {

				if (max < Es[l + j] / Ei[l + j - 42])
					max = Es[l + j] / Ei[l + j - 42];
			}

			// floor max

			max = Math.floor(max);

			for (int j = 0; j < sampleSize; j++) {
				if (Ei[l + j - 42] * max < Es[l + j]) {
					final int position = (l + j);

					bandnum = (int) Math
							.round(Ei[l + j - 42] / (bandsize + minEi));

					// make certain that the rounded value is within the correct
					// bounds

					if (NUM_BANDS <= bandnum)
						bandnum = NUM_BANDS - 1;
					else if (bandnum < 0)
						bandnum = 0;

					// add the beat to an array of all beats to be processed
					// later (a lower limit is set to reduce noise)
					// only add the beat if it is greater than LOWEST_BAND
					// so that the midi converter does not have to use
					// notes that are almost indistinguishable.

					if (LOWEST_BAND < bandnum)
						beats.add(new Beat(bandnum, position));
				}
			}
		}
	}

	/**
	 * Return the beats.
	 * 
	 * @return Beats.
	 */

	public ArrayList<Beat> getBeats() {
		return beats;
	}

	/**
	 * Get the conversion between Subbands and Frequencies.
	 * 
	 * @return The covnersion between the two.
	 */

	public double getSubbandToFrequency() {
		return subbandToFrequency;
	}
}
