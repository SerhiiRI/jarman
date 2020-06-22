#[allow(dead_code)]
pub mod escapes {
    pub const BOLD:        &str = "\x1B[1m";
    pub const DIM:         &str = "\x1B[2m";
    pub const UNDERLINED:  &str = "\x1B[4m";
    pub const BLINK:       &str = "\x1B[5m";
    pub const REVERSE:     &str = "\x1B[7m";
    pub const HIDDEN:      &str = "\x1B[8m";

    pub const RESET:       &str = "\x1B[0m";

    pub const DEFAULT_FG:  &str = "\x1B[39m";
    pub const BLACK:       &str = "\x1B[30m";
    pub const RED:         &str = "\x1B[31m";
    pub const GREEN:       &str = "\x1B[32m";
    pub const YELLOW:      &str = "\x1B[33m";
    pub const BLUE:        &str = "\x1B[34m";
    pub const MAGENTA:     &str = "\x1B[35m";
    pub const CYAN:        &str = "\x1B[36m";
    pub const LIGHT_GRAY:  &str = "\x1B[37m";

    pub const DARK_GRAY:     &str = "\x1B[90m";
    pub const LIGHT_RED:     &str = "\x1B[91m";
    pub const LIGHT_GREEN:   &str = "\x1B[92m";
    pub const LIGHT_YELLOW:  &str = "\x1B[93m";
    pub const LIGHT_BLUE:    &str = "\x1B[94m";
    pub const LIGHT_MAGENTA: &str = "\x1B[95m";
    pub const LIGHT_CYAN:    &str = "\x1B[96m";
    pub const WHITE:         &str = "\x1B[97m";

    pub const CURSOR_UP:       &str = "\x1B[{}A";
    pub const CURSOR_DOWN:     &str = "\x1B[{}B";
    pub const CURSOR_FORWARD:  &str = "\x1B[{}C";
    pub const CURSOR_BACKWARD: &str = "\x1B[{}D";
    pub const CURSOR_PUT:      &str = "\x1B[{};{}H";

    pub const CURSOR_NEXT_LINE:&str = "\x1B[{}E";
    pub const CURSOR_PREV_LINE:&str = "\x1B[{}F";
    pub const CURSOR_HORIZONTAL_ABSOLUTE:&str = "\x1B[{}G"; //moved horizontal in current line
    pub const CURSOR_VERTICAL_ABSOLUTE:  &str = "\x1B[{}d"; //moved verticaly in current columnt

    pub const CURSOR_ENABLE_BLINCKING:  &str = "\x1B[12h";
    pub const CURSOR_DISABLE_BLINCKING: &str = "\x1B[12l";
    pub const CURSOR_SHOW:&str = "\x1B[25h";
    pub const CURSOR_HIDE:&str = "\x1B[25l";

    // x1B[{} J pattern to clearing screen.
    // x1B[ 1 J: erase from start to cursor.
    // x1B[ 2 J: erase whole display.
    // x1B[ 3 J: erase whole display including scroll-back
    pub const ERASE_DISPLAY_FROM_START_TO_CURSOR:  &str = "\x1B[1J";
    pub const ERASE_DISPLAY:                       &str = "\x1B[2J";
    pub const ERASE_DISPLAY_WITH_SCROLL:           &str = "\x1B[3J";

    // x1B[ K: Erase line (default: from cursor to end of line).
    // x1B[ 1 K: erase from start of line to cursor.
    // x1B[ 2 K: erase whole line.
    pub const ERASE_LINE_FROM_CURSOR_TO_END:       &str = "\x1B[K";
    pub const ERASE_LINE_FROM_START_TO_CURSOR:     &str = "\x1B[1K";
    pub const ERASE_LINE:                          &str = "\x1B[2K";

    pub const INSERT_CHARACTER:  &str = "\x1B[{}@"; // Insert {} spaces at the current cursor position, shifting all existing text to the right. Text exiting the screen to the right is removed.
    pub const DELETE_CHARACTER:  &str = "\x1B[{}P"; // Delete {} characters at the current cursor position, shifting in space characters from the right edge of the screen.
    pub const ERASE_CHARACTER:   &str = "\x1B[{}X"; // Erase {} characters from the current cursor position by overwriting them with a space character.
    pub const INSERT_LINES:      &str = "\x1B[{}L"; // Inserts {} lines into the buffer at the cursor position. The line the cursor is on, and lines below it, will be shifted downwards
    pub const DELETE_LINES:      &str = "\x1B[{}M"; // Deletes {} lines from the buffer, starting with the row the cursor is on.

    pub const SOFT_RESET_TERMINAL_SETTINGS: &str = "\x1B[!p";


    // DEC Line drawing mode
    pub const DEC_MODE_ENABLE:  &str = "\x1B(0";
    pub const DEC_MODE_DISABLE: &str = "\x1B(B";
    // box charset
    pub const DEC_6A: &str = "0x6a"; // ┘
    pub const DEC_6B: &str = "0x6b"; // ┐
    pub const DEC_6C: &str = "0x6c"; // ┌
    pub const DEC_6D: &str = "0x6d"; // └
    pub const DEC_6E: &str = "0x6e"; // ┼
    pub const DEC_71: &str = "0x71"; // ─
    pub const DEC_74: &str = "0x74"; // ├
    pub const DEC_75: &str = "0x75"; // ┤
    pub const DEC_76: &str = "0x76"; // ┴
    pub const DEC_77: &str = "0x77"; // ┬
    pub const DEC_78: &str = "0x78"; // │
        //┌──────┬──────┐
        //│   1  │   2  │
        //├──────┼──────┤
        //│   1  │   2  │
        //├──────┴──────┤
        //│  for        │
        //│   drawing   │
        //│     boxes   │
        //│             │
        //└─────────────┘
}

// #[allow(dead_code)]
// pub mod escapes_hint{
//     macro_rules! escape {
//         () => ("".to_string());
//         ($fmt:ty, $($arg:expr)* ) => ({format!($fmt, $($arg)*)});
//     }
// }