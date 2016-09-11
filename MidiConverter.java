package karma;

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

public class MidiConverter {

	public static int CHANNEL = 1;

	public static int VELOCITY = 127;

	public static int MAX_WAIT = 400;

	public static int NOTE_SIZE_ESTIMATE = 20;

	/**
	 * http://newt.phys.unsw.edu.au/jw/notes.html
	 * 
	 * @param frequency
	 * @return
	 */

	private int frequencytoNote(double frequency) {
		return (int) (12.0 * Math.log(frequency / 440.0) / Math.log(2) + 69.0);
	}

	public void beatsToSong(ArrayList<List<Integer>> beatList,
			double subbandToFrequency, double bitToTime)
					throws InvalidMidiDataException {
		// create the synthesizer

		Synthesizer player = null;

		try {
			player = MidiSystem.getSynthesizer();

			player.open();
		} catch (MidiUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// get the channels

		MidiChannel chan[] = player.getChannels();

		// start playing the song

		double frequency = 0;

		int note;

		long waitTime;

		for (int i = 0; i < beatList.size(); i++) {
			
			frequency = beatList.get(i).get(1) * subbandToFrequency / 10.0;

			note = frequencytoNote(frequency);

			// Midi notes end at 127 
			
			if (note <= 127)
				chan[CHANNEL].noteOn(note, VELOCITY);
			else
				chan[CHANNEL].noteOn(127, VELOCITY);

			// give the note time to play based on the bytes between
			// this note and the next one
			// if the last note is being played, play it for the same
			// time as the previous note

			if (i + 1 < beatList.size())
				waitTime = (long) (beatList.get(i + 1).get(0) * bitToTime * 60.0
						* 1000.0 * NOTE_SIZE_ESTIMATE);
			else
				waitTime = (long) (beatList.get(i).get(0) * bitToTime * 60.0
						* 1000.0 * NOTE_SIZE_ESTIMATE);

			// helps to move the song along by limiting the maximum amount of
			// wait time

			if (MAX_WAIT < waitTime)
				waitTime = MAX_WAIT;

			try {
				if (i + 1 < beatList.size())
					Thread.sleep(waitTime);
				else
					Thread.sleep(waitTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// turn the note off

			chan[CHANNEL].noteOff(note);
		}

		player.close();
	}
}
