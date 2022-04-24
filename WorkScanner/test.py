from tkinter import *


class YesOrControl(Frame):
    def __init__(self, parent, yesorno=bool(0)):
        Frame.__init__(self, parent)

        color = "#ffd699"
        if yesorno:
            color = "#80ffcc"

        c1 = IntVar()
        self.chk1 = Checkbutton(self, variable=c1, selectcolor=color)
        c2 = IntVar()
        self.chk2 = Checkbutton(self, variable=c2, selectcolor=color)
        c3 = IntVar()
        self.chk3 = Checkbutton(self, variable=c3, selectcolor=color)
        c4 = IntVar()
        self.chk4 = Checkbutton(self, variable=c4, selectcolor=color)
        c5 = IntVar()
        self.chk5 = Checkbutton(self, variable=c5, selectcolor=color)

        self.chk1.grid(row=0, column=0, sticky="ew")
        self.chk2.grid(row=1, column=0, sticky="ew")
        self.chk3.grid(row=2, column=0, sticky="ew")
        self.chk4.grid(row=3, column=0, sticky="ew")
        self.chk5.grid(row=4, column=0, sticky="ew")


class YesNoControl(Frame):
    def __init__(self, parent):
        Frame.__init__(self, parent)

        self.yes = YesOrControl(self, bool(1))
        self.no = YesOrControl(self, bool(0))

        self.yes.grid(row=0, column=0, sticky="ns")
        self.no.grid(row=0, column=1, sticky="ns")


class LetterControl(Frame):
    def __init__(self, parent, letter="A"):
        Frame.__init__(self, parent)

        self.label = Label(self, text=letter, anchor="w", font=("Arial", 32))
        c = IntVar(value=1)
        c.set(1)
        self.select = Checkbutton(self, variable=c)
        self.select.select()
        self.yesNo = YesNoControl(self)

        self.label.grid(row=0, column=0, columnspan=2)
        self.select.grid(row=1, column=0, columnspan=2)
        self.yesNo.grid(row=2, column=1)


class Example(Frame):
    def __init__(self, parent):
        Frame.__init__(self, parent)

        self.l1 = LetterControl(self, "A")
        self.l2 = LetterControl(self, "B")
        self.l3 = LetterControl(self, "C")
        self.l4 = LetterControl(self, "D")
        self.l5 = LetterControl(self, "E")
        self.l6 = LetterControl(self, "F")
        self.l7 = LetterControl(self, "G")
        self.l8 = LetterControl(self, "H")
        self.l9 = LetterControl(self, "I")
        self.l10 = LetterControl(self, "J")
        self.l11 = LetterControl(self, "K")
        self.l12 = LetterControl(self, "L")
        self.l13 = LetterControl(self, "M")
        self.l14 = LetterControl(self, "N")
        self.l15 = LetterControl(self, "O")
        self.l16 = LetterControl(self, "P")
        self.l17 = LetterControl(self, "Q")
        self.l18 = LetterControl(self, "R")
        self.l19 = LetterControl(self, "S")
        self.l20 = LetterControl(self, "T")
        self.l21 = LetterControl(self, "U")
        self.l22 = LetterControl(self, "V")
        self.l23 = LetterControl(self, "W")
        self.l24 = LetterControl(self, "X")
        self.l25 = LetterControl(self, "Y")
        self.l26 = LetterControl(self, "Z")
        self.list = Listbox(self)

        self.l1.grid(row=0, column=0, sticky="ns")
        self.l2.grid(row=0, column=1, sticky="ns")
        self.l3.grid(row=0, column=2, sticky="ns")
        self.l4.grid(row=0, column=3, sticky="ns")
        self.l5.grid(row=0, column=4, sticky="ns")
        self.l6.grid(row=0, column=5, sticky="ns")
        self.l7.grid(row=0, column=6, sticky="ns")
        self.l8.grid(row=0, column=7, sticky="ns")
        self.l9.grid(row=0, column=8, sticky="ns")
        self.l10.grid(row=0, column=9, sticky="ns")
        self.l11.grid(row=0, column=10, sticky="ns")
        self.l12.grid(row=0, column=11, sticky="ns")
        self.l13.grid(row=0, column=12, sticky="ns")
        self.l14.grid(row=0, column=13, sticky="ns")
        self.l15.grid(row=0, column=14, sticky="ns")
        self.l16.grid(row=0, column=15, sticky="ns")
        self.l17.grid(row=0, column=16, sticky="ns")
        self.l18.grid(row=0, column=17, sticky="ns")
        self.l19.grid(row=0, column=18, sticky="ns")
        self.l20.grid(row=0, column=19, sticky="ns")
        self.l21.grid(row=0, column=20, sticky="ns")
        self.l22.grid(row=0, column=21, sticky="ns")
        self.l23.grid(row=0, column=22, sticky="ns")
        self.l24.grid(row=0, column=23, sticky="ns")
        self.l25.grid(row=0, column=24, sticky="ns")
        self.l26.grid(row=0, column=25, sticky="ns")
        self.list.grid(row=1, column=0, columnspan=26, sticky="nsew", pady=10, padx=5)

        self.columnconfigure(index=0, weight=0)
        self.rowconfigure(index=1, weight=1)


if __name__ == "__main__":
    root = Tk()
    root.title("Scanner")
    root.geometry("1420x600")
    Example(root).place(x=0, y=0, relwidth=1, relheight=1)
    root.mainloop()
