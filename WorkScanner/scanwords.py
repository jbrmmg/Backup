import tkinter as tk
from tkinter import font

MEMORIES = 4
LETTERS_IN_THE_ALPHABET = 26
ASCII_OF_A = 65
WORD_LENGTH = 5
F1_KEYCODE = 67
SHIFT_L_KEYCODE = 50
SHIFT_R_KEYCODE = 62
ALT_KEYCODE = 64
ESCAPE_KEYCODE = 9


# noinspection PyTypeChecker,PyArgumentList
class PlaceFlags(tk.Frame):
    def __init__(self, parent, include):
        tk.Frame.__init__(self, parent)

        self.memory = 0
        self.memory_values = [[0 for _ in range(WORD_LENGTH)] for _ in range(MEMORIES)]
        self._observers = []
        for x in range(MEMORIES):
            for y in range(WORD_LENGTH):
                self.memory_values[x][y] = 0

        self.colour = "#ffcc80"
        if include == 1:
            self.colour = "#99ffbb"

        self.enabled = tk.IntVar(value=1)
        self.flags = [tk.Checkbutton] * WORD_LENGTH
        self.values = [tk.IntVar] * WORD_LENGTH
        for x in range(WORD_LENGTH):
            self.values[x] = tk.IntVar(value=0)
            self.flags[x] = tk.Checkbutton(self, variable=self.values[x], selectcolor=self.colour, command=self.change)
            self.flags[x].grid(row=x, column=0)

    def inform(self):
        for callback in self._observers:
            callback()

    def bind_to(self, callback):
        self._observers.append(callback)

    def memorychange(self, newmemory):
        if newmemory != self.memory:
            for x in range(WORD_LENGTH):
                self.memory_values[self.memory][x] = self.values[x].get()

            self.memory = newmemory
            for x in range(WORD_LENGTH):
                self.values[x].set(value=self.memory_values[self.memory][x])

    def enable(self, selected):
        if selected == 1:
            self.enabled.set(1)
        else:
            self.enabled.set(0)
        for x in range(WORD_LENGTH):
            if self.enabled.get() == 1:
                self.flags[x].config(state=tk.NORMAL)
            else:
                self.flags[x].config(state=tk.DISABLED)
                self.values[x].set(value=0)

    def pattern(self, letter):
        result = ""

        for x in range(WORD_LENGTH):
            if self.values[x].get() == 1:
                result = result + letter
            else:
                result = result + "_"

        return result

    def isused(self, letter):
        for x in range(WORD_LENGTH):
            if self.values[x].get() == 1:
                return letter

        return ""

    def reset(self):
        for x in range(WORD_LENGTH):
            self.values[x].set(0)
        self.enable(1)

    def include(self, letter, index):
        if self.values[index].get() == 1:
            return letter

        return ""

    def change(self):
        self.inform()

    def letter_press(self, index):
        if index in range(WORD_LENGTH):
            if self.values[index].get() == 1:
                self.values[index].set(0)
            else:
                self.values[index].set(1)

            self.inform()


class LetterWidget(tk.Frame):
    def __init__(self, parent, letter):
        tk.Frame.__init__(self, parent)

        self._observers = []
        self.memory = 0
        self.letter = letter
        self.memory_values = [0 for _ in range(MEMORIES)]
        for x in range(MEMORIES):
            self.memory_values[x] = 1

        self.label = tk.Label(self, text=letter, font=("Ariel", 32))
        self.label.grid(row=0, column=0, columnspan=2)

        self.checkvar = tk.IntVar(value=1)
        self.cb = tk.Checkbutton(self, variable=self.checkvar, command=self.enable)
        self.cb.grid(row=1, column=0, columnspan=2)

        self.checkvar2 = tk.IntVar(value=0)
        self.cb2 = tk.Checkbutton(self, variable=self.checkvar2, command=self.enable)
        self.cb2.grid(row=2, column=0, columnspan=2)

        self.includeFlags = PlaceFlags(self, 1)
        self.includeFlags.grid(row=3, column=0)
        self.includeFlags.bind_to(self.inform)

        self.excludeFlags = PlaceFlags(self, 0)
        self.excludeFlags.grid(row=3, column=1)
        self.excludeFlags.bind_to(self.inform)

    def inform(self):
        for callback in self._observers:
            callback()

    def bind_to(self, callback):
        self._observers.append(callback)

    def enable(self):
        if self.checkvar.get() == 1:
            self.cb2.config(state=tk.NORMAL)
        else:
            self.checkvar2.set(0)
            self.cb2.config(state=tk.DISABLED)

        self.includeFlags.enable(self.checkvar.get())
        self.excludeFlags.enable(self.checkvar.get())
        self.inform()

    def isenabled(self):
        if self.checkvar.get() == 0:
            return self.label.cget("text")

        return ""

    def reset(self):
        self.checkvar.set(1)
        self.checkvar2.set(0)
        self.cb2.config(state=tk.NORMAL)
        self.includeFlags.reset()
        self.excludeFlags.reset()

    def isused(self):
        if self.checkvar2.get() == 1:
            return self.label.cget("text")

        return self.excludeFlags.isused(self.label.cget("text"))

    def excludepattern(self):
        return self.excludeFlags.pattern(self.label.cget("text"))

    def include(self, index):
        return self.includeFlags.include(self.label.cget("text"), index)

    def memorychange(self, newmemory):
        if newmemory != self.memory:
            self.memory_values[self.memory] = self.checkvar.get()
            self.includeFlags.memorychange(newmemory)
            self.excludeFlags.memorychange(newmemory)

            self.memory = newmemory
            self.checkvar.set(value=self.memory_values[self.memory])

            self.includeFlags.enable(self.checkvar.get())
            self.excludeFlags.enable(self.checkvar.get())

    def letter_press(self, key: str, shift, function):
        if key.upper() == self.letter.upper():
            if shift is False and function == -1:
                if self.checkvar.get() == 1:
                    self.checkvar.set(0)
                else:
                    self.checkvar.set(1)
                self.enable()
            else:
                if function != -1:
                    if not shift:
                        self.excludeFlags.letter_press(function)
                    else:
                        self.includeFlags.letter_press(function)


class LetterCount:
    def __init__(self, index):
        self.letter_index = chr(ASCII_OF_A + index)
        self.count = 0

    def increment(self):
        self.count = self.count + 1

    def __lt__(self, other):
        return self.count > other.count

    def get_letter_index(self):
        return self.letter_index


# noinspection PyTypeChecker,PyArgumentList
class MainFrame(tk.Frame):
    def __init__(self, parent):
        tk.Frame.__init__(self, parent)

        self.letterByPosn = [LetterCount] * LETTERS_IN_THE_ALPHABET
        for x in range(LETTERS_IN_THE_ALPHABET):
            self.letterByPosn[x] = LetterCount(x)

        with open("words.txt") as fp:
            line = fp.readline()
            while line:
                letterlist = list(line)
                for x in letterlist:
                    intx = ord(x)
                    intx = intx - ASCII_OF_A
                    if intx in range(LETTERS_IN_THE_ALPHABET):
                        self.letterByPosn[intx].increment()

                line = fp.readline()

        self.fn_key = -1
        self.shift = False
        self.alt = False
        self.letterByPosn = sorted(self.letterByPosn)
        self.letter = [LetterWidget] * LETTERS_IN_THE_ALPHABET
        for x in range(LETTERS_IN_THE_ALPHABET):
            self.letter[x] = LetterWidget(self, self.letterByPosn[x].get_letter_index())
            self.letter[x].grid(row=0, column=x)
            self.letter[x].bind_to(self.evaluate)

        self.trigger = tk.Button(self, text="Evaluate", command=self.evaluate)
        self.trigger.grid(row=1, column=0, columnspan=4)

        self.reset = tk.Button(self, text="Reset", command=self.reset_screen)
        self.reset.grid(row=1, column=4, columnspan=4)

        self.mvalue = tk.IntVar(value=0)
        self.m1btn = tk.Radiobutton(self, variable=self.mvalue, value=0, command=self.selectionchange)
        self.m1btn.grid(row=1, column=20)

        self.m2btn = tk.Radiobutton(self, variable=self.mvalue, value=1, command=self.selectionchange)
        self.m2btn.grid(row=1, column=21)

        self.m3btn = tk.Radiobutton(self, variable=self.mvalue, value=2, command=self.selectionchange)
        self.m3btn.grid(row=1, column=22)

        self.m4btn = tk.Radiobutton(self, variable=self.mvalue, value=3, command=self.selectionchange)
        self.m4btn.grid(row=1, column=23)

        list_font = font.Font(family="Courier", size=10)
        self.list = tk.Listbox(self, font=list_font)
        self.list.grid(row=2, column=0, columnspan=LETTERS_IN_THE_ALPHABET, sticky="nsew", padx=5, pady=5)

        self.columnconfigure(0, weight=1)
        self.rowconfigure(2, weight=1)

        self.scroll = tk.Scrollbar(self.list, orient=tk.VERTICAL)
        self.scroll.pack(side=tk.RIGHT, fill="y")
        self.list.config(yscrollcommand=self.scroll.set)
        self.scroll.config(command=self.list.yview)

    def selectionchange(self):
        for x in range(LETTERS_IN_THE_ALPHABET):
            self.letter[x].memorychange(self.mvalue.get())

        self.evaluate()

    def reset_screen(self):
        self.list.delete(0, tk.END)

        for x in range(LETTERS_IN_THE_ALPHABET):
            self.letter[x].reset()

    def reset_keys(self):
        self.shift = False
        self.alt = False
        self.fn_key = -1

    def key_pressed(self, e):
        print(str(e.keycode) + " " + str(self.alt) + " " + str(self.shift) + " " + str(self.fn_key))

        if e.keycode == ESCAPE_KEYCODE:
            if self.alt:
                self.reset_keys()
                self.reset_screen()
                return

            self.reset_keys()
            return

        if len(e.char) > 0:
            if ord(e.char.upper()) - ASCII_OF_A in range(LETTERS_IN_THE_ALPHABET):
                for x in range(LETTERS_IN_THE_ALPHABET):
                    self.letter[x].letter_press(e.char, self.shift, self.fn_key)

                self.reset_keys()
                return

        if e.keycode == SHIFT_L_KEYCODE or e.keycode == SHIFT_R_KEYCODE:
            self.shift = True
            return

        if self.alt:
            if e.keycode - F1_KEYCODE in range(MEMORIES):
                self.reset_keys()
                self.mvalue.set(e.keycode - F1_KEYCODE)
                self.selectionchange()
                return

        if e.keycode - F1_KEYCODE in range(WORD_LENGTH):
            self.fn_key = e.keycode - F1_KEYCODE
            return

        if e.keycode == ALT_KEYCODE:
            self.alt = True
            return

        self.reset_keys()

    @staticmethod
    def checkcriteria(word, exclude, pattern, include, exclude_patterns):
        word_letters = list(word)

        if len(word_letters) != WORD_LENGTH:
            return False

        # Does the word contain a letter that it should not?
        for next_exclude_letter in exclude:
            for next_letter in word_letters:
                if next_exclude_letter == next_letter:
                    return False

        # Does the word match the pattern
        for pattern_index in range(WORD_LENGTH):
            if pattern[pattern_index] != '_':
                if pattern[pattern_index] != word_letters[pattern_index]:
                    return False

        # Does the word contain a must include letter?
        for next_include_letter in include:
            includes = False

            for next_letter in word_letters:
                if next_letter == next_include_letter:
                    includes = True

            if not includes:
                return False

        # Does this word match the excluded pattern
        for next_exclude_pattern in exclude_patterns:
            for pattern_index in range(WORD_LENGTH):
                if next_exclude_pattern[pattern_index] != '_':
                    if next_exclude_pattern[pattern_index] == word_letters[pattern_index]:
                        return False

        return True

    def evaluate(self):
        self.list.delete(0, tk.END)

        unused = ""
        for x in range(LETTERS_IN_THE_ALPHABET):
            unused = unused + self.letter[x].isenabled()
        self.list.insert(tk.END, unused)

        used = ""
        for x in range(LETTERS_IN_THE_ALPHABET):
            used = used + self.letter[x].isused()
        self.list.insert(tk.END, used)

        full_exclude_pattern = ""
        excludes = []
        for x in range(LETTERS_IN_THE_ALPHABET):
            exclude_pattern = self.letter[x].excludepattern()
            if exclude_pattern != "_____":
                full_exclude_pattern = full_exclude_pattern + exclude_pattern + " "
                excludes.append(list(exclude_pattern))
        self.list.insert(tk.END, full_exclude_pattern)

        include_pattern = ""
        for x in range(WORD_LENGTH):
            include = ""
            for y in range(LETTERS_IN_THE_ALPHABET):
                include = self.letter[y].include(x)
                if include != "":
                    break

            if include != "":
                include_pattern = include_pattern + include
            else:
                include_pattern = include_pattern + "_"
        self.list.insert(tk.END, include_pattern)

        next_line = ""
        next_line_count = 0
        with open("words.txt") as fp:
            line = fp.readline()
            cnt = 0
            while line:
                if self.checkcriteria(line.strip().upper(), list(unused), list(include_pattern), list(used), excludes):
                    cnt += 1
                    next_line = next_line + line.strip() + " "
                    next_line_count = next_line_count + 1
                    if next_line_count == 28:
                        self.list.insert(tk.END, next_line)
                        next_line = ""
                        next_line_count = 0

                line = fp.readline()

            self.list.insert(tk.END, next_line)
            self.list.insert(tk.END, str(cnt))


if __name__ == "__main__":
    window = tk.Tk()
    window.title("Word Scanner")
    window.geometry("1400x800")
    frame = MainFrame(window)
    frame.place(x=0, y=0, relwidth=1, relheight=1)
    window.bind("<KeyPress>", frame.key_pressed)
    window.mainloop()
