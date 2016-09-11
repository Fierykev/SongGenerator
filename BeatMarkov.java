package karma;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

public class BeatMarkov {

	/**
	 * Use the class like a structure to store beat data.
	 * 
	 * @author Kevin
	 *
	 */

	class Beat {
		private final int beat, position;

		public Beat(int beat, int position) {
			this.beat = beat;

			this.position = position;
		}

		public int getBeat() {
			return beat;
		}

		public int getPosition() {
			return position;
		}
	}
	
	public void Beat()
	{
		
	}

	/**
	 * Converts the Beats to a HashMap. The key for the HashMap is a List with
	 * the length of the analysis level and contains a list of Beat bands from
	 * the song. The element stored under the key is a second HashMap containing
	 * a Integer key paired with a List of Integers. The Integer key represents
	 * a Beat band location that follows the List of Beat bands in the outer
	 * HashMap. List of Integers represents the following information about the
	 * Beat band in the following order: index zero: the sum of the timing
	 * between this beat and any beat that comes before it. This is a SUM of
	 * delta T for ALL beats that fall into this band and follow the List of
	 * Beat bands specified earlier. This is done so that the average delta T
	 * can be taken in outputMarkov. index one: The number of beats that fall
	 * into this band and follow the the List of Beat bands specified earlier.
	 * 
	 * @param input
	 *            The String version of the input file.
	 * @return The HashMap filled with the data from the file input String.
	 */

	@SuppressWarnings("resource")
	public HashMap<List<Integer>, HashMap<Integer, List<Integer>>> inputToMap(
			ArrayList<Beat> input, int analysisLevel) {
		// convert the input file into a hashmap

		HashMap<List<Integer>, HashMap<Integer, List<Integer>>> markovHash = new HashMap<List<Integer>, HashMap<Integer, List<Integer>>>();

		// HashMap<Integer,
		// List<Integer>>
		// as (sum of spacing between
		// this beat and the last beat,
		// number of times beat occurs)

		Beat lastBeat, nxtBeat;

		for (int beatPos = 0; beatPos < input.size()
				- analysisLevel; beatPos++) {
			// get the next key for the map
			// if the analysisLevel is zero, make the key equal to a new
			// Integer[analysisLevel]

			// parse the Beat to store its elements into the map

			List<Integer> key = new ArrayList<Integer>();

			List<Integer> value = new ArrayList<Integer>();

			for (int i = 0; i < analysisLevel; i++)
				key.add(analysisLevel != 0
						? input.get(beatPos + i).beat
						: new Integer(0));

			// get the Beat the ends the key

			lastBeat = input.get(beatPos + analysisLevel - 1);

			// get the Beat that follow the key

			nxtBeat = input.get(beatPos + analysisLevel);

			// make sure the key exists

			if (!markovHash.containsKey(key))
				markovHash.put(key, new HashMap<Integer, List<Integer>>());

			// make sure the nxtChars key exists and alter the number of times
			// the set of Beats appears

			if (!markovHash.get(key).containsKey(nxtBeat.beat)) {

				// store the space between this beat and the last one as well
				// as the number of times it has occurred (1)

				value.add(nxtBeat.position - lastBeat.position); // delta beats

				value.add(1); // times occurred

				markovHash.get(key).put(nxtBeat.beat, value);
			} else {
				// store the space between this beat and the last one as well
				// as the number of times it has occurred

				// delta beats

				value.add(nxtBeat.position - lastBeat.position
						+ markovHash.get(key).get(nxtBeat.beat).get(0));

				// times occurred

				value.add(1 + markovHash.get(key).get(nxtBeat.beat).get(1));

				markovHash.get(key).put(nxtBeat.beat, value);
			}
		}

		return markovHash;
	}

	/**
	 * Uses a HashMap that is created by inputToMap() to output a text which
	 * bases its characters on a source material. The ultimate goal is to
	 * execute a Markov algorithm based on the source material.
	 * 
	 * @param length
	 *            The length of the output ArrayList.
	 * @param markovHash
	 *            A HashMap that contains the data needed by the Markov
	 *            algorithm. More information about the HashMap structure and
	 *            meaning of the data can be found at the inputToMap() method.
	 */

	@SuppressWarnings("resource")
	public ArrayList<List<Integer>> runMarkov(int length, int analysisLevel,
			HashMap<List<Integer>, HashMap<Integer, List<Integer>>> markovHash) {

		// choose a random seed

		List<Integer> currentKey = new ArrayList<Integer>();

		// fill with negative values (bands cannot be negative)

		for (int i = 0; i < analysisLevel; i++)
			currentKey.add(new Integer(-1));

		Random r;

		r = new Random();

		// create the output (List index 0 is the space between this beat and
		// the last beat, index 1 is the beat band).

		ArrayList<List<Integer>> output = new ArrayList<List<Integer>>();

		// loop until the characters output to the file are no longer less than
		// what the user asked for.

		while (output.size() < length) {
			// find the next character to copy

			if (currentKey != null && markovHash.containsKey(currentKey)) {

				// find new key

				// create the probabilities

				int probSum = 0; // the sum of all of the occurrences of String
									// in the original text

				// use the HashMap to find the sum since its faster than
				// searching
				// through the original string again

				for (Entry<Integer, List<Integer>> pair : markovHash
						.get(currentKey).entrySet())
					probSum += pair.getValue().get(1);

				// choose a random key that is between 0 and the sum of the
				// String's
				// occurrences in the original text

				int keyRandom = r.nextInt(probSum);

				probSum = 0; // reset the sum of the keys

				// loops until the sum of the keys is greater than the random
				// value.
				// When the sum of the keys is greater than the random value,
				// the new key is found.

				for (Entry<Integer, List<Integer>> pair : markovHash
						.get(currentKey).entrySet()) {
					probSum += (int) pair.getValue().get(1);

					if (keyRandom < probSum) {
						// write the new Beat to the output with its timing

						List<Integer> toOut = new ArrayList<Integer>();

						toOut.add(pair.getValue().get(0)
								/ pair.getValue().get(1)); // the average of the
															// delta beat times

						toOut.add(pair.getKey()); // the beat band

						output.add(toOut); // add the beat data to the output

						// create the new key
						// factor in the edge case of the
						// analysis level being zero
						// in the edge case of zero, do not update the key

						if (analysisLevel != 0) {
							currentKey.remove(0);

							currentKey.add(pair.getKey());
						}

						break;
					}
				}
			} else {
				// choose a new seed by randomly choosing a map Key

				// convert the map to a list so that it can easily be searched
				// by
				// an index value

				List<List<Integer>> mapList = new ArrayList<List<Integer>>(
						markovHash.keySet());

				// grab a random key based on random index value

				final int randomIndex = r.nextInt(mapList.size());

				// deep copy the elements into the current key

				for (int i = 0; i < analysisLevel; i++)
					currentKey.set(i,
							new Integer(mapList.get(randomIndex).get(i)));

				// DO NOT WRITE the seed to the file as it does not contain
				// the timings between beats
			}
		}

		return output;
	}
}
