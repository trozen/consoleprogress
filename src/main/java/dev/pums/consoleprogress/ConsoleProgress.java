package dev.pums.consoleprogress;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

public class ConsoleProgress {
	private static final boolean consoleOutput;
	private static final boolean ansiEnabled;
	private static final Ansi ansi;
	private static final String ESC = "" + (char)27;
	private static final String CSI = ESC + "[";
	private static int installCount = 0;
	private static final Object lock = new Object();
	private static PrintStream originalSystemOut;
	private static PrintStream originalSystemErr;
	private static LineBufferedOutputStream bufferedErrorStream;
	private static LineBufferedOutputStream bufferedOutputStream;
	private static String currentProgress;

	static {
		consoleOutput = detectConsoleOutput();
		ansiEnabled = (consoleOutput && System.getenv().get("TERM") != null);
		ansi = new Ansi();
	}

	private static boolean detectConsoleOutput() {
		return System.console() != null;
	}

	public static boolean isConsoleOutput() {
		return consoleOutput;
	}

	public static boolean isAnsiEnabled() {
		return ansiEnabled;
	}

	public static void installSystemStreams() {
		synchronized (lock) {
			installCount++;
			if (installCount != 1) {
				return;
			}
			if (!consoleOutput) {
				return;
			}
			originalSystemOut = System.out;
			originalSystemErr = System.err;
			bufferedOutputStream = new LineBufferedOutputStream(originalSystemOut);
			System.setOut(new PrintStream(bufferedOutputStream));
			bufferedErrorStream = new LineBufferedOutputStream(originalSystemErr);
			System.setErr(new PrintStream(bufferedErrorStream));
		}
	}

	public static void uninstallSystemStreams() {
		synchronized (lock) {
			installCount--;
			if (installCount != 0) {
				return;
			}
			if (!consoleOutput) {
				return;
			}
			try {
				if (bufferedOutputStream != null) {
					bufferedOutputStream.writeBuffer();
				}
				if (bufferedErrorStream != null) {
					bufferedErrorStream.writeBuffer();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.setOut(originalSystemOut);
			System.setErr(originalSystemErr);
		}
	}

	public static void show(String text) {
		synchronized (lock) {
			if (!consoleOutput) {
				return;
			}
			int previousLength = 0;
			if (currentProgress != null) {
				previousLength = currentProgress.length();
			}
			originalSystemOut.write('\r');
			int currentLength = 0;
			if (text != null) {
				currentLength = text.length();
				originalSystemOut.print(text);
			}
			int fillCount = previousLength - currentLength;
			clearChars(fillCount);
			currentProgress = text;
			originalSystemOut.flush();
		}
	}

	public static void hide() {
		show(null);
	}

	public static Ansi ansi() {
		return ansi;
	}

	private static void clearProgressLineUnsafe() {
		if (currentProgress == null) {
			return;
		}
		originalSystemOut.write('\r');
		clearChars(currentProgress.length());
		originalSystemOut.flush();
	}

	private static void restoreProgressUnsafe() {
		if (currentProgress == null) {
			return;
		}
		originalSystemOut.print(currentProgress);
		originalSystemOut.flush();
	}

	private static void clearChars(int fillCount) {
		for (int i = 0; i < fillCount; i++) {
			originalSystemOut.write(' ');
		}
		for (int i = 0; i < fillCount; i++) {
			originalSystemOut.write('\b');
		}
	}

	public static final class Ansi {
		public AnsiStyleBuilder style() {
			return new AnsiStyleBuilder();
		}
	}

	public static final class AnsiStyleBuilder {
		private StringBuilder sb;

		private AnsiStyleBuilder() {
		}

		@Override
		public String toString() {
			if (sb == null) {
				return "";
			}
			return CSI + sb.toString() + "m";
		}

		private void append(String code) {
			if (!ansiEnabled) {
				return;
			}
			if (sb == null) {
				sb = new StringBuilder();
			} else {
				sb.append(';');
			}
			sb.append(code);
		}

		public AnsiStyleBuilder reset() {
			append("0");
			return this;
		}

		public AnsiStyleBuilder bold() {
			append("1");
			return this;
		}

		public AnsiStyleBuilder underline() {
			append("4");
			return this;
		}

		public AnsiStyleBuilder fgBlack() {
			append("30");
			return this;
		}

		public AnsiStyleBuilder fgRed() {
			append("31");
			return this;
		}

		public AnsiStyleBuilder fgGreen() {
			append("32");
			return this;
		}

		public AnsiStyleBuilder fgYellow() {
			append("33");
			return this;
		}

		public AnsiStyleBuilder fgBlue() {
			append("34");
			return this;
		}

		public AnsiStyleBuilder fgMagenta() {
			append("35");
			return this;
		}

		public AnsiStyleBuilder fgCyan() {
			append("36");
			return this;
		}

		public AnsiStyleBuilder fgWhite() {
			append("37");
			return this;
		}

		public AnsiStyleBuilder fgDefault() {
			append("39");
			return this;
		}

		public AnsiStyleBuilder bgBlack() {
			append("40");
			return this;
		}

		public AnsiStyleBuilder bgRed() {
			append("41");
			return this;
		}

		public AnsiStyleBuilder bgGreen() {
			append("42");
			return this;
		}

		public AnsiStyleBuilder bgYellow() {
			append("43");
			return this;
		}

		public AnsiStyleBuilder bgBlue() {
			append("44");
			return this;
		}

		public AnsiStyleBuilder bgMagenta() {
			append("45");
			return this;
		}

		public AnsiStyleBuilder bgCyan() {
			append("46");
			return this;
		}

		public AnsiStyleBuilder bgWhite() {
			append("47");
			return this;
		}

		public AnsiStyleBuilder bgDefault() {
			append("49");
			return this;
		}
	}

	private static final class LineBufferedOutputStream extends OutputStream {
		private final OutputStream output;
		private byte[] buffer = new byte[4096];
		private int count;

		public LineBufferedOutputStream(OutputStream output) {
			this.output = output;
		}

		@Override
		public void write(int i) throws IOException {
			if (count == buffer.length) {
				buffer = Arrays.copyOf(buffer, buffer.length * 2);
			}
			buffer[count] = (byte)i;
			count++;
			if (i == '\n') {
				synchronized (lock) {
					clearProgressLineUnsafe();
					writeBuffer();
					restoreProgressUnsafe();
				}
			}
		}

		public void writeBuffer() throws IOException {
			output.write(buffer, 0, count);
			count = 0;
		}
	}
}
