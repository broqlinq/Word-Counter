package rs.raf.kids.kwc.cli.command;

public interface Command {

    String getName();

    void execute(String...args);

    Command INVALID = new Command() {
        @Override
        public String getName() {
            return "Invalid command";
        }

        @Override
        public void execute(String... args) {
            throw new UnsupportedOperationException("Invalid command");
        }
    };
}
