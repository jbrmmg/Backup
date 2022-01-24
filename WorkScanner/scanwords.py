exclude = list("EARTHISCU")
pattern = list("_____")
include = list("LNK")
exclude_patterns = [list("L_NK_"),list("_L_NK")]


def check_criteria(word):
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


with open("words.txt") as fp:
    line = fp.readline()
    cnt = 0
    while line:
        if check_criteria(line.strip().upper()):
            cnt += 1
            print("Line {}: {}".format(line.strip(), cnt))

        line = fp.readline()
