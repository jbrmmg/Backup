import tkinter as tk
from tkinter import font

class PlaceFlags(tk.Frame):
    def __init__(self, parent, include):
        tk.Frame.__init__(self, parent)

        self.colour = "#ffcc80"
        if(include == 1):
            self.colour = "#99ffbb"

        self.enabled = tk.IntVar(value=1)
        self.flags = [0 for x in range(5)]
        self.values = [0 for x in range(5)]
        for x in range(5):
            self.values[x] = tk.IntVar(value=0)
            self.flags[x] = tk.Checkbutton(self, variable=self.values[x], selectcolor=self.colour)
            self.flags[x].grid(row=x, column=0)

    def Enable(self):
        for x in range(5):
            if(self.enabled.get() == 1):
                self.flags[x].config(state=tk.DISABLED)
                self.values[x].set(0)
            else:
                self.flags[0].config(state=tk.NORMAL)
        if(self.enabled.get() == 1):
            self.enabled.set(0)
        else:
            self.enabled.set(1)

    def Pattern(self, letter):
        result = ""

        for x in range(5):
            if(self.values[x].get() == 1):
                result = result + letter
            else:
                result = result + "_"

        return result

    def IsUsed(self, letter):
        for x in range(5):
            if(self.values[x].get() == 1):
                return letter

        return ""

    def Reset(self):
        for x in range(5):
            self.values[x].set(0)

    def Include(self, letter, index):
        if(self.values[index].get() == 1):
            return letter

        return ""


class LetterWidget(tk.Frame):
    def __init__(self, parent, index):
        tk.Frame.__init__(self, parent)

        self.label = tk.Label(self, text=chr(65+index), font=("Ariel", 32))
        self.label.grid(row=0, column=0, columnspan=2)

        self.CheckVar = tk.IntVar(value=1)
        self.cb = tk.Checkbutton(self, variable=self.CheckVar, command=self.Enable)
        self.cb.grid(row=1, column=0, columnspan=2)

        self.includeFlags = PlaceFlags(self,1)
        self.includeFlags.grid(row=2, column=0)

        self.excludeFlags = PlaceFlags(self,0)
        self.excludeFlags.grid(row=2, column=1)

    def Enable(self):
        self.includeFlags.Enable()
        self.excludeFlags.Enable()

    def IsEnabled(self):
        if(self.CheckVar.get() == 0):
            return self.label.cget("text")

        return ""

    def Reset(self):
        self.CheckVar.set(1)
        self.includeFlags.Reset()
        self.excludeFlags.Reset()

    def IsUsed(self):
        return self.excludeFlags.IsUsed(self.label.cget("text"))

    def ExcludePattern(self):
        return self.excludeFlags.Pattern(self.label.cget("text"))

    def Include(self,index):
        return self.includeFlags.Include(self.label.cget("text"), index)


class MainFrame(tk.Frame):
    def __init__(self, parent):
        tk.Frame.__init__(self, parent)

        self.letter = [0 for x in range(26)]
        for x in range(26):
            self.letter[x] = LetterWidget(self, x)
            self.letter[x].grid(row=0, column=x)

        self.trigger = tk.Button(self, text="Evaluate", command=self.Evaluate)
        self.trigger.grid(row=1, column=0, columnspan=4)

        self.reset = tk.Button(self, text="Reset", command=self.Reset)
        self.reset.grid(row=1, column=4, columnspan=4)

        listFont = font.Font(family="Courier", size=10)
        self.list = tk.Listbox(self, font=listFont)
        self.list.grid(row=2, column=0, columnspan=26, sticky="nsew", padx=5, pady=5)

        self.columnconfigure(0,weight=1)
        self.rowconfigure(2,weight=1)

        self.scroll = tk.Scrollbar(self.list, orient=tk.VERTICAL)
        self.scroll.pack(side=tk.RIGHT,fill="y")
        self.list.config(yscrollcommand=self.scroll.set)
        self.scroll.config(command=self.list.yview)

    def Reset(self):
        self.list.delete(0, tk.END)

        for x in range(26):
            self.letter[x].Reset()

    def Evaluate(self):
        self.list.delete(0, tk.END)

        unsed = ""
        for x in range(26):
            unused = unsed + self.letter[x].IsEnabled()
        self.list.insert(tk.END, unused)

        used = ""
        for x in range(26):
            used = used + self.letter[x].IsUsed()
        self.list.insert(tk.END, used)

        fullExcludePattern = ""
        for x in range(26):
            excludePattern = self.letter[x].ExcludePattern()
            if(excludePattern != "_____"):
                fullExcludePattern = fullExcludePattern + excludePattern + " "
        self.list.insert(tk.END, fullExcludePattern)

        includePattern = ""
        for x in range(5):
            include = ""
            for y in range(26):
                include = self.letter[y].Include(x)
                if(include != ""):
                    break

                if(include != ""):
                    includePattern = includePattern + include
                else:
                    includePattern = includePattern + "_"
        self.list.insert(tk.END, includePattern)

        for x in range(40):
            self.list.insert(tk.END, "EARTH EMOJI WITCH FRAME CHAUX PLANE")


if __name__ == "__main__":
    window = tk.Tk()
    window.geometry("1460x800")
    MainFrame(window).place(x=0, y=0, relwidth=1, relheight=1)
    window.mainloop()
