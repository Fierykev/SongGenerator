package karma;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Karma extends BeatMarkov {

	final static boolean DEBUG = true;

	final static int ANALYSIS_LEVEL = 10;

	final static int OUTPUT_LENGTH = 5000;

	private AudioInputStream audioInputStream;

	private byte[] waveData;

	BeatDetection beatDetection;

	public static void main(String[] args) {

		Karma beatDetection;

		if (DEBUG) {
			beatDetection = new Karma(
					"Audio/To_Aru_Kaguku_no_Railgun Mono 16.wav");
		} else {
			if (args.length != 1)
			{
				System.out.println("This program takes one paramater:\n"
						+ " the location of a 16-bits, mono, little endian, wave file");
				
				System.exit(-1);
			}

			beatDetection = new Karma(args[0]);
		}

		try {
			beatDetection.wavetoBytes();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		beatDetection.analyzeWave();

		beatDetection.runMarkov();
	}

	public Karma(String filename) {
		audioInputStream = null;

		try {
			audioInputStream = AudioSystem
					.getAudioInputStream(new File(filename));
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// check that the file is a 16-bits, mono, BigEndian, wav file.

		if (audioInputStream.getFormat()
				.getEncoding() == Encoding.PCM_UNSIGNED) {
			System.err.println("The file does not have PCM signed encoding\n"
					+ " and, thus, is most likely not a wave file");

			System.exit(-1);
		}

		if (audioInputStream.getFormat().isBigEndian()) {
			System.err.println("The file is not little endian.");

			System.exit(-1);
		}

		if (audioInputStream.getFormat().getSampleSizeInBits() != 16) {
			System.err.println("The input file is not 16 bits.");

			System.exit(-1);
		}

		if (audioInputStream.getFormat().getChannels() != 1) {
			System.err.println("The input file is not mono.");

			System.exit(-1);
		}
	}

	/**
	 * Get all of the byte data in the wave file.
	 * 
	 * @throws IOException
	 *             File could not be read.
	 */

	public void wavetoBytes() throws IOException {
		// store the data in bytes

		waveData = new byte[audioInputStream.available()];

		audioInputStream.read(waveData);
	}

	/**
	 * Analyze the wave file and find its beats.
	 */

	public void analyzeWave() {
		beatDetection = new BeatDetection();

		beatDetection.analyzeWave(audioInputStream, waveData);
	}

	/**
	 * Filter the beats to find their length.
	 */

	public void runMarkov() {

		ArrayList<Beat> beats = beatDetection.getBeats();

		double subbandToFrequency = beatDetection.getSubbandToFrequency();

		if (beats.size() < ANALYSIS_LEVEL) {
			System.out.println("Too few beats in the song to"
					+ " run a Markov algorithm on");

			System.exit(-1);
		}

		// convert the input file into a HashMap

		HashMap<List<Integer>, HashMap<Integer, List<Integer>>> markovHash = inputToMap(
				beats, ANALYSIS_LEVEL);

		// run the Markov algorith and generate an ArrayList with a List of
		// Integers containing the
		// timing between this beat and the last beat as well as the
		// beat's band

		ArrayList<List<Integer>> beatList = runMarkov(OUTPUT_LENGTH,
				ANALYSIS_LEVEL, markovHash);

		// generate the song

		MidiConverter midiConv = new MidiConverter();

		// write the midi

		final double bitToTime = 1.0
				/ audioInputStream.getFormat().getSampleSizeInBits()
				/ audioInputStream.getFormat().getSampleRate();

		try {
			midiConv.beatsToSong(beatList, subbandToFrequency, bitToTime);
		} catch (InvalidMidiDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
