import tkinter as tk
from tkinter import font


# noinspection PyTypeChecker,PyArgumentList
class PlaceFlags(tk.Frame):
    def __init__(self, parent, include):
        tk.Frame.__init__(self, parent)

        self.colour = "#ffcc80"
        if include == 1:
            self.colour = "#99ffbb"

        self.enabled = tk.IntVar(value=1)
        self.flags = [tk.Checkbutton] * 5
        self.values = [tk.IntVar] * 5
        for x in range(5):
            self.values[x] = tk.IntVar(value=0)
            self.flags[x] = tk.Checkbutton(self, variable=self.values[x], selectcolor=self.colour)
            self.flags[x].grid(row=x, column=0)

    def enable(self):
        for x in range(5):
            if self.enabled.get() == 1:
                self.flags[x].config(state=tk.DISABLED)
                self.values[x].set(value=0)
            else:
                self.flags[x].config(state=tk.NORMAL)
        if self.enabled.get() == 1:
            self.enabled.set(0)
        else:
            self.enabled.set(1)

    def pattern(self, letter):
        result = ""

        for x in range(5):
            if self.values[x].get() == 1:
                result = result + letter
            else:
                result = result + "_"

        return result

    def isused(self, letter):
        for x in range(5):
            if self.values[x].get() == 1:
                return letter

        return ""

    def reset(self):
        for x in range(5):
            self.values[x].set(0)
        self.enabled.set(0)
        self.enable()

    def include(self, letter, index):
        if self.values[index].get() == 1:
            return letter

        return ""


class LetterWidget(tk.Frame):
    def __init__(self, parent, index):
        tk.Frame.__init__(self, parent)

        self.label = tk.Label(self, text=chr(65+index), font=("Ariel", 32))
        self.label.grid(row=0, column=0, columnspan=2)

        self.CheckVar = tk.IntVar(value=1)
        self.cb = tk.Checkbutton(self, variable=self.CheckVar, command=self.enable)
        self.cb.grid(row=1, column=0, columnspan=2)

        self.includeFlags = PlaceFlags(self, 1)
        self.includeFlags.grid(row=2, column=0)

        self.excludeFlags = PlaceFlags(self, 0)
        self.excludeFlags.grid(row=2, column=1)

    def enable(self):
        self.includeFlags.enable()
        self.excludeFlags.enable()

    def isenabled(self):
        if self.CheckVar.get() == 0:
            return self.label.cget("text")

        return ""

    def reset(self):
        self.CheckVar.set(1)
        self.includeFlags.reset()
        self.excludeFlags.reset()

    def isused(self):
        return self.excludeFlags.isused(self.label.cget("text"))

    def excludepattern(self):
        return self.excludeFlags.pattern(self.label.cget("text"))

    def include(self, index):
        return self.includeFlags.include(self.label.cget("text"), index)


# noinspection PyTypeChecker,PyArgumentList
class MainFrame(tk.Frame):
    def __init__(self, parent):
        tk.Frame.__init__(self, parent)

        self.letter = [LetterWidget] * 26
        for x in range(26):
            self.letter[x] = LetterWidget(self, x)
            self.letter[x].grid(row=0, column=x)

        self.trigger = tk.Button(self, text="Evaluate", command=self.evaluate)
        self.trigger.grid(row=1, column=0, columnspan=4)

        self.reset = tk.Button(self, text="Reset", command=self.reset)
        self.reset.grid(row=1, column=4, columnspan=4)

        list_font = font.Font(family="Courier", size=10)
        self.list = tk.Listbox(self, font=list_font)
        self.list.grid(row=2, column=0, columnspan=26, sticky="nsew", padx=5, pady=5)

        self.columnconfigure(0, weight=1)
        self.rowconfigure(2, weight=1)

        self.scroll = tk.Scrollbar(self.list, orient=tk.VERTICAL)
        self.scroll.pack(side=tk.RIGHT, fill="y")
        self.list.config(yscrollcommand=self.scroll.set)
        self.scroll.config(command=self.list.yview)

    def reset(self):
        self.list.delete(0, tk.END)

        for x in range(26):
            self.letter[x].reset()

    @staticmethod
    def checkcriteria(word, exclude, pattern, include, exclude_patterns):
        word_letters = list(word)

        if len(word_letters) != 5:
            return False

        # Does the word contain a letter that it should not?
        for next_exclude_letter in exclude:
            for next_letter in word_letters:
                if next_exclude_letter == next_letter:
                    return False

        # Does the word match the pattern
        for pattern_index in range(0, 5):
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
            for pattern_index in range(0, 5):
                if next_exclude_pattern[pattern_index] != '_':
                    if next_exclude_pattern[pattern_index] == word_letters[pattern_index]:
                        return False

        return True

    def evaluate(self):
        self.list.delete(0, tk.END)

        unused = ""
        for x in range(26):
            unused = unused + self.letter[x].isenabled()
        self.list.insert(tk.END, unused)

        used = ""
        for x in range(26):
            used = used + self.letter[x].isused()
        self.list.insert(tk.END, used)

        full_exclude_pattern = ""
        excludes = []
        for x in range(26):
            exclude_pattern = self.letter[x].excludepattern()
            if exclude_pattern != "_____":
                full_exclude_pattern = full_exclude_pattern + exclude_pattern + " "
                excludes.append(list(exclude_pattern))
        self.list.insert(tk.END, full_exclude_pattern)

        include_pattern = ""
        for x in range(5):
            include = ""
            for y in range(26):
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
                    if next_line_count == 30:
                        self.list.insert(tk.END, next_line)
                        next_line = ""
                        next_line_count = 0

                line = fp.readline()

            self.list.insert(tk.END, next_line)
            self.list.insert(tk.END, str(cnt))


if __name__ == "__main__":
    window = tk.Tk()
    window.geometry("1460x800")
    MainFrame(window).place(x=0, y=0, relwidth=1, relheight=1)
    window.mainloop()
