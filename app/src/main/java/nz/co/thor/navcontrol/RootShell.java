package nz.co.thor.navcontrol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

final class RootShell {
    private RootShell() {}

    static Result run(String command) {
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) output.append('\n');
                    output.append(line);
                }
            }

            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new Result(false, "Root command timed out");
            }
            return new Result(process.exitValue() == 0, output.toString().trim());
        } catch (Exception e) {
            if (process != null) process.destroyForcibly();
            return new Result(false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    static final class Result {
        final boolean ok;
        final String output;

        Result(boolean ok, String output) {
            this.ok = ok;
            this.output = output;
        }
    }
}
