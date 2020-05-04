package dev.pums.consoleprogress;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

// TODO: check if stderr is redirected -- if yes, do not show progress

public class Demo {
	public static void main(String[] args) {
		System.out.println("ConsoleOutput: " + ConsoleProgress.isConsoleOutput());
		System.out.println("ANSIEnabled: " + ConsoleProgress.isAnsiEnabled());

		System.out.println("Visual styles:");
		final ConsoleProgress.Ansi ansi = ConsoleProgress.ansi();
		System.out.println(ansi.style().bold().fgYellow() + "brightYellow" + ansi.style().reset());
		System.out.println(ansi.style().bgCyan().underline().fgBlack() + "underlineBlackOnCyan" + ansi.style().reset());

		try {
			System.out.println(" ==== running 'demo1'...");
			demo1();
			System.out.println(" ==== running 'demo2'...");
			demo2();
			System.out.println(" ==== running 'demo3'...");
			demo3();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void demo1() throws InterruptedException {
		try {
			ConsoleProgress.installSystemStreams();

			final int max = 50;
			for (int i = 0; i <= max; i++) {
				if ((i % 5) == 3) {
					System.out.print("xxx");
				}
				if ((i % 5) == 4) {
					System.out.println("yyy");
				}
				if ((i % 6) == 5) {
					System.err.print("err");
				}
				if ((i % 12) == 11) {
					System.err.println();
				}

				Thread.sleep(50);
				ConsoleProgress.show(demo1Progress(i, max));
			}
			ConsoleProgress.hide();
			Thread.sleep(200);

			System.out.println("Done");
		} finally {
			ConsoleProgress.uninstallSystemStreams();
		}
	}

	private static String demo1Progress(int step, int max) {
		if (step < 10) {
			final char[] spinChars = {'/', '-', '\\', '|'};
			final char spin = spinChars[step % 4];
			return "[ " + spin + " waiting...                                     ]";
		} else {
			final StringBuilder sb = new StringBuilder();
			sb.append("[");
			for (int j = 0; j < max; j++) {
				sb.append(step > j ? "=" : " ");
			}
			sb.append("] ").append(step).append("/").append(max);
			return sb.toString();
		}
	}

	private static void demo2() throws InterruptedException {
		try {
			ConsoleProgress.installSystemStreams();

			System.out.println("Begin");

			final CountDownLatch countDownLatch = new CountDownLatch(2);

			new Thread(() -> {
				try {
					for (int i = 0; i < 10; i++) {
						Thread.sleep(200);
						System.out.print("xxx");
						Thread.sleep(200);
						System.out.println("yyy");
					}
				} catch (Throwable e) {
					e.printStackTrace();
				} finally {
					countDownLatch.countDown();
				}
			}).start();
			new Thread(() -> {
				try {
					for (int i = 0; i < 10; i++) {
						Thread.sleep(300);
						System.err.print("err");
						if ((i % 3 == 2)) {
							System.err.println();
						}
					}
					System.err.println();
				} catch (Throwable e) {
					e.printStackTrace();
				} finally {
					countDownLatch.countDown();
				}
			}).start();

			final int max = 50;
			for (int i = 0; i <= max; i++) {
				Thread.sleep(50);
				ConsoleProgress.show(demo1Progress(i, max));
			}
			ConsoleProgress.hide();
			Thread.sleep(200);

			countDownLatch.await();

			System.out.println("Done");
		} finally {
			ConsoleProgress.uninstallSystemStreams();
		}
	}

	private static void demo3() throws InterruptedException {
		final int begin = 167;
		final int end = 700;
		final int total = 5384;
		final int step = 12;
		final int width = 50;
		final Duration totalTime = Duration.ofMinutes(7);
		final Duration beginTime = Duration.ofSeconds(13);
		try {
			ConsoleProgress.installSystemStreams();
			final ConsoleProgress.Ansi ansi = ConsoleProgress.ansi();
			Duration elapsed = beginTime;
			for (int cur = begin; cur <= end; cur += step) {
				final StringBuilder sb = new StringBuilder();
				sb.append(String.format("Reading %3d%% ", cur * 100 / total));
				sb.append(ansi.style().fgYellow()).append("│");
				sb.append(solidProgress(width, (double)cur / (double)total));
				sb.append("│").append(ansi.style().reset());
				sb.append(String.format(" %4d/%4dMB", cur, total));
				sb.append(" (").append(formatDuration(elapsed)).append(" / ");
				sb.append(formatDuration(totalTime.minus(elapsed))).append(")");
				ConsoleProgress.show(sb.toString());
				Thread.sleep(250);
				elapsed = elapsed.plus(Duration.ofMillis(250));
			}
			ConsoleProgress.hide();
		} finally {
			ConsoleProgress.uninstallSystemStreams();
		}
	}

	private static String formatDuration(Duration d) {
		long s = d.getSeconds();
		return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
	}

	private static String solidProgress(int width, double perc) {
		final char[] fillChars = " ▏▎▍▌▋▊▉█".toCharArray();
		final char[] chars = new char[width];
		final int fillCharCount = fillChars.length - 1;
		for (int i = 0; i < width; i++) {
			int x = (int)((width * perc - i) * fillCharCount);
			x = Math.max(0, Math.min(fillCharCount, x));
			chars[i] = fillChars[x];
		}
		return new String(chars);
	}
}
