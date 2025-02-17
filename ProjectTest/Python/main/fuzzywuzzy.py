'''
fuzzywuzzy/string_processing.py
'''
from __future__ import unicode_literals
import re
import string
import sys

PY3 = sys.version_info[0] == 3
if PY3:
    string = str


class StringProcessor(object):
    """
    This class defines method to process strings in the most
    efficient way. Ideally all the methods below use unicode strings
    for both input and output.
    """

    regex = re.compile(r"(?ui)\W")

    @classmethod
    def replace_non_letters_non_numbers_with_whitespace(cls, a_string):
        """
        This function replaces any sequence of non letters and non
        numbers with a single white space.
        """
        return cls.regex.sub(" ", a_string)

    strip = staticmethod(string.strip)
    to_lower_case = staticmethod(string.lower)
    to_upper_case = staticmethod(string.upper)


'''
fuzzywuzzy/fuzz.py
'''
#!/usr/bin/env python
# encoding: utf-8
from __future__ import unicode_literals
import platform
import warnings

try:
    from .StringMatcher import StringMatcher as SequenceMatcher
except ImportError:
    if platform.python_implementation() != "PyPy":
        warnings.warn('Using slow pure-python SequenceMatcher. Install python-Levenshtein to remove this warning')
    from difflib import SequenceMatcher

import utils


###########################
# Basic Scoring Functions #
###########################

@utils.check_for_none
@utils.check_for_equivalence
@utils.check_empty_string
def ratio(s1, s2):
    s1, s2 = utils.make_type_consistent(s1, s2)

    m = SequenceMatcher(None, s1, s2)
    return utils.intr(100 * m.ratio())


@utils.check_for_none
@utils.check_for_equivalence
@utils.check_empty_string
def partial_ratio(s1, s2):
    """"Return the ratio of the most similar substring
    as a number between 0 and 100."""
    s1, s2 = utils.make_type_consistent(s1, s2)

    if len(s1) <= len(s2):
        shorter = s1
        longer = s2
    else:
        shorter = s2
        longer = s1

    m = SequenceMatcher(None, shorter, longer)
    blocks = m.get_matching_blocks()

    # each block represents a sequence of matching characters in a string
    # of the form (idx_1, idx_2, len)
    # the best partial match will block align with at least one of those blocks
    #   e.g. shorter = "abcd", longer = XXXbcdeEEE
    #   block = (1,3,3)
    #   best score === ratio("abcd", "Xbcd")
    scores = []
    for block in blocks:
        long_start = block[1] - block[0] if (block[1] - block[0]) > 0 else 0
        long_end = long_start + len(shorter)
        long_substr = longer[long_start:long_end]

        m2 = SequenceMatcher(None, shorter, long_substr)
        r = m2.ratio()
        if r > .995:
            return 100
        else:
            scores.append(r)

    return utils.intr(100 * max(scores))


##############################
# Advanced Scoring Functions #
##############################

def _process_and_sort(s, force_ascii, full_process=True):
    """Return a cleaned string with token sorted."""
    # pull tokens
    ts = utils.full_process(s, force_ascii=force_ascii) if full_process else s
    tokens = ts.split()

    # sort tokens and join
    sorted_string = u" ".join(sorted(tokens))
    return sorted_string.strip()


# Sorted Token
#   find all alphanumeric tokens in the string
#   sort those tokens and take ratio of resulting joined strings
#   controls for unordered string elements
@utils.check_for_none
def _token_sort(s1, s2, partial=True, force_ascii=True, full_process=True):
    sorted1 = _process_and_sort(s1, force_ascii, full_process=full_process)
    sorted2 = _process_and_sort(s2, force_ascii, full_process=full_process)

    if partial:
        return partial_ratio(sorted1, sorted2)
    else:
        return ratio(sorted1, sorted2)


def token_sort_ratio(s1, s2, force_ascii=True, full_process=True):
    """Return a measure of the sequences' similarity between 0 and 100
    but sorting the token before comparing.
    """
    return _token_sort(s1, s2, partial=False, force_ascii=force_ascii, full_process=full_process)


def partial_token_sort_ratio(s1, s2, force_ascii=True, full_process=True):
    """Return the ratio of the most similar substring as a number between
    0 and 100 but sorting the token before comparing.
    """
    return _token_sort(s1, s2, partial=True, force_ascii=force_ascii, full_process=full_process)


@utils.check_for_none
def _token_set(s1, s2, partial=True, force_ascii=True, full_process=True):
    """Find all alphanumeric tokens in each string...
        - treat them as a set
        - construct two strings of the form:
            <sorted_intersection><sorted_remainder>
        - take ratios of those two strings
        - controls for unordered partial matches"""

    if not full_process and s1 == s2:
        return 100

    p1 = utils.full_process(s1, force_ascii=force_ascii) if full_process else s1
    p2 = utils.full_process(s2, force_ascii=force_ascii) if full_process else s2

    if not utils.validate_string(p1):
        return 0
    if not utils.validate_string(p2):
        return 0

    # pull tokens
    tokens1 = set(p1.split())
    tokens2 = set(p2.split())

    intersection = tokens1.intersection(tokens2)
    diff1to2 = tokens1.difference(tokens2)
    diff2to1 = tokens2.difference(tokens1)

    sorted_sect = " ".join(sorted(intersection))
    sorted_1to2 = " ".join(sorted(diff1to2))
    sorted_2to1 = " ".join(sorted(diff2to1))

    combined_1to2 = sorted_sect + " " + sorted_1to2
    combined_2to1 = sorted_sect + " " + sorted_2to1

    # strip
    sorted_sect = sorted_sect.strip()
    combined_1to2 = combined_1to2.strip()
    combined_2to1 = combined_2to1.strip()

    if partial:
        ratio_func = partial_ratio
    else:
        ratio_func = ratio

    pairwise = [
        ratio_func(sorted_sect, combined_1to2),
        ratio_func(sorted_sect, combined_2to1),
        ratio_func(combined_1to2, combined_2to1)
    ]
    return max(pairwise)


def token_set_ratio(s1, s2, force_ascii=True, full_process=True):
    return _token_set(s1, s2, partial=False, force_ascii=force_ascii, full_process=full_process)


def partial_token_set_ratio(s1, s2, force_ascii=True, full_process=True):
    return _token_set(s1, s2, partial=True, force_ascii=force_ascii, full_process=full_process)


###################
# Combination API #
###################

# q is for quick
def QRatio(s1, s2, force_ascii=True, full_process=True):
    """
    Quick ratio comparison between two strings.

    Runs full_process from utils on both strings
    Short circuits if either of the strings is empty after processing.

    :param s1:
    :param s2:
    :param force_ascii: Allow only ASCII characters (Default: True)
    :full_process: Process inputs, used here to avoid double processing in extract functions (Default: True)
    :return: similarity ratio
    """

    if full_process:
        p1 = utils.full_process(s1, force_ascii=force_ascii)
        p2 = utils.full_process(s2, force_ascii=force_ascii)
    else:
        p1 = s1
        p2 = s2

    if not utils.validate_string(p1):
        return 0
    if not utils.validate_string(p2):
        return 0

    return ratio(p1, p2)


def UQRatio(s1, s2, full_process=True):
    """
    Unicode quick ratio

    Calls QRatio with force_ascii set to False

    :param s1:
    :param s2:
    :return: similarity ratio
    """
    return QRatio(s1, s2, force_ascii=False, full_process=full_process)


# w is for weighted
def WRatio(s1, s2, force_ascii=True, full_process=True):
    """
    Return a measure of the sequences' similarity between 0 and 100, using different algorithms.

    **Steps in the order they occur**

    #. Run full_process from utils on both strings
    #. Short circuit if this makes either string empty
    #. Take the ratio of the two processed strings (fuzz.ratio)
    #. Run checks to compare the length of the strings
        * If one of the strings is more than 1.5 times as long as the other
          use partial_ratio comparisons - scale partial results by 0.9
          (this makes sure only full results can return 100)
        * If one of the strings is over 8 times as long as the other
          instead scale by 0.6

    #. Run the other ratio functions
        * if using partial ratio functions call partial_ratio,
          partial_token_sort_ratio and partial_token_set_ratio
          scale all of these by the ratio based on length
        * otherwise call token_sort_ratio and token_set_ratio
        * all token based comparisons are scaled by 0.95
          (on top of any partial scalars)

    #. Take the highest value from these results
       round it and return it as an integer.

    :param s1:
    :param s2:
    :param force_ascii: Allow only ascii characters
    :type force_ascii: bool
    :full_process: Process inputs, used here to avoid double processing in extract functions (Default: True)
    :return:
    """

    if full_process:
        p1 = utils.full_process(s1, force_ascii=force_ascii)
        p2 = utils.full_process(s2, force_ascii=force_ascii)
    else:
        p1 = s1
        p2 = s2

    if not utils.validate_string(p1):
        return 0
    if not utils.validate_string(p2):
        return 0

    # should we look at partials?
    try_partial = True
    unbase_scale = .95
    partial_scale = .90

    base = ratio(p1, p2)
    len_ratio = float(max(len(p1), len(p2))) / min(len(p1), len(p2))

    # if strings are similar length, don't use partials
    if len_ratio < 1.5:
        try_partial = False

    # if one string is much much shorter than the other
    if len_ratio > 8:
        partial_scale = .6

    if try_partial:
        partial = partial_ratio(p1, p2) * partial_scale
        ptsor = partial_token_sort_ratio(p1, p2, full_process=False) \
            * unbase_scale * partial_scale
        ptser = partial_token_set_ratio(p1, p2, full_process=False) \
            * unbase_scale * partial_scale

        return utils.intr(max(base, partial, ptsor, ptser))
    else:
        tsor = token_sort_ratio(p1, p2, full_process=False) * unbase_scale
        tser = token_set_ratio(p1, p2, full_process=False) * unbase_scale

        return utils.intr(max(base, tsor, tser))


def UWRatio(s1, s2, full_process=True):
    """Return a measure of the sequences' similarity between 0 and 100,
    using different algorithms. Same as WRatio but preserving unicode.
    """
    return WRatio(s1, s2, force_ascii=False, full_process=full_process)


'''
fuzzywuzzy/utils.py
'''
from __future__ import unicode_literals
import sys
import functools

from string_processing import StringProcessor


PY3 = sys.version_info[0] == 3


def validate_string(s):
    """
    Check input has length and that length > 0

    :param s:
    :return: True if len(s) > 0 else False
    """
    try:
        return len(s) > 0
    except TypeError:
        return False


def check_for_equivalence(func):
    @functools.wraps(func)
    def decorator(*args, **kwargs):
        if args[0] == args[1]:
            return 100
        return func(*args, **kwargs)
    return decorator


def check_for_none(func):
    @functools.wraps(func)
    def decorator(*args, **kwargs):
        if args[0] is None or args[1] is None:
            return 0
        return func(*args, **kwargs)
    return decorator


def check_empty_string(func):
    @functools.wraps(func)
    def decorator(*args, **kwargs):
        if len(args[0]) == 0 or len(args[1]) == 0:
            return 0
        return func(*args, **kwargs)
    return decorator


bad_chars = str("").join([chr(i) for i in range(128, 256)])  # ascii dammit!
if PY3:
    translation_table = dict((ord(c), None) for c in bad_chars)
    unicode = str


def asciionly(s):
    if PY3:
        return s.translate(translation_table)
    else:
        return s.translate(None, bad_chars)


def asciidammit(s):
    if type(s) is str:
        return asciionly(s)
    elif type(s) is unicode:
        return asciionly(s.encode('ascii', 'ignore'))
    else:
        return asciidammit(unicode(s))


def make_type_consistent(s1, s2):
    """If both objects aren't either both string or unicode instances force them to unicode"""
    if isinstance(s1, str) and isinstance(s2, str):
        return s1, s2

    elif isinstance(s1, unicode) and isinstance(s2, unicode):
        return s1, s2

    else:
        return unicode(s1), unicode(s2)


def full_process(s, force_ascii=False):
    """Process string by
        -- removing all but letters and numbers
        -- trim whitespace
        -- force to lower case
        if force_ascii == True, force convert to ascii"""

    if force_ascii:
        s = asciidammit(s)
    # Keep only Letters and Numbers (see Unicode docs).
    string_out = StringProcessor.replace_non_letters_non_numbers_with_whitespace(s)
    # Force into lowercase.
    string_out = StringProcessor.to_lower_case(string_out)
    # Remove leading and trailing whitespaces.
    string_out = StringProcessor.strip(string_out)
    return string_out


def intr(n):
    '''Returns a correctly rounded integer'''
    return int(round(n))


'''
fuzzywuzzy/__init__.py
'''
# -*- coding: utf-8 -*-
__version__ = '0.18.0'


'''
fuzzywuzzy/StringMatcher.py
'''
#!/usr/bin/env python
# encoding: utf-8
"""
StringMatcher.py

ported from python-Levenshtein
[https://github.com/miohtama/python-Levenshtein]
License available here: https://github.com/miohtama/python-Levenshtein/blob/master/COPYING
"""

from Levenshtein import *
from warnings import warn


class StringMatcher:
    """A SequenceMatcher-like class built on the top of Levenshtein"""

    def _reset_cache(self):
        self._ratio = self._distance = None
        self._opcodes = self._editops = self._matching_blocks = None

    def __init__(self, isjunk=None, seq1='', seq2=''):
        if isjunk:
            warn("isjunk not NOT implemented, it will be ignored")
        self._str1, self._str2 = seq1, seq2
        self._reset_cache()

    def set_seqs(self, seq1, seq2):
        self._str1, self._str2 = seq1, seq2
        self._reset_cache()

    def set_seq1(self, seq1):
        self._str1 = seq1
        self._reset_cache()

    def set_seq2(self, seq2):
        self._str2 = seq2
        self._reset_cache()

    def get_opcodes(self):
        if not self._opcodes:
            if self._editops:
                self._opcodes = opcodes(self._editops, self._str1, self._str2)
            else:
                self._opcodes = opcodes(self._str1, self._str2)
        return self._opcodes

    def get_editops(self):
        if not self._editops:
            if self._opcodes:
                self._editops = editops(self._opcodes, self._str1, self._str2)
            else:
                self._editops = editops(self._str1, self._str2)
        return self._editops

    def get_matching_blocks(self):
        if not self._matching_blocks:
            self._matching_blocks = matching_blocks(self.get_opcodes(),
                                                    self._str1, self._str2)
        return self._matching_blocks

    def ratio(self):
        if not self._ratio:
            self._ratio = ratio(self._str1, self._str2)
        return self._ratio

    def quick_ratio(self):
        # This is usually quick enough :o)
        if not self._ratio:
            self._ratio = ratio(self._str1, self._str2)
        return self._ratio

    def real_quick_ratio(self):
        len1, len2 = len(self._str1), len(self._str2)
        return 2.0 * min(len1, len2) / (len1 + len2)

    def distance(self):
        if not self._distance:
            self._distance = distance(self._str1, self._str2)
        return self._distance


'''
fuzzywuzzy/process.py
'''
#!/usr/bin/env python
# encoding: utf-8
import fuzz
import utils
import heapq
import logging
from functools import partial


default_scorer = fuzz.WRatio


default_processor = utils.full_process


def extractWithoutOrder(query, choices, processor=default_processor, scorer=default_scorer, score_cutoff=0):
    """Select the best match in a list or dictionary of choices.

    Find best matches in a list or dictionary of choices, return a
    generator of tuples containing the match and its score. If a dictionary
    is used, also returns the key for each match.

    Arguments:
        query: An object representing the thing we want to find.
        choices: An iterable or dictionary-like object containing choices
            to be matched against the query. Dictionary arguments of
            {key: value} pairs will attempt to match the query against
            each value.
        processor: Optional function of the form f(a) -> b, where a is the query or
            individual choice and b is the choice to be used in matching.

            This can be used to match against, say, the first element of
            a list:

            lambda x: x[0]

            Defaults to fuzzywuzzy.utils.full_process().
        scorer: Optional function for scoring matches between the query and
            an individual processed choice. This should be a function
            of the form f(query, choice) -> int.

            By default, fuzz.WRatio() is used and expects both query and
            choice to be strings.
        score_cutoff: Optional argument for score threshold. No matches with
            a score less than this number will be returned. Defaults to 0.

    Returns:
        Generator of tuples containing the match and its score.

        If a list is used for choices, then the result will be 2-tuples.
        If a dictionary is used, then the result will be 3-tuples containing
        the key for each match.

        For example, searching for 'bird' in the dictionary

        {'bard': 'train', 'dog': 'man'}

        may return

        ('train', 22, 'bard'), ('man', 0, 'dog')
    """
    # Catch generators without lengths
    def no_process(x):
        return x

    try:
        if choices is None or len(choices) == 0:
            return
    except TypeError:
        pass

    # If the processor was removed by setting it to None
    # perfom a noop as it still needs to be a function
    if processor is None:
        processor = no_process

    # Run the processor on the input query.
    processed_query = processor(query)

    if len(processed_query) == 0:
        logging.warning(u"Applied processor reduces input query to empty string, "
                        "all comparisons will have score 0. "
                        "[Query: \'{0}\']".format(query))

    # Don't run full_process twice
    if scorer in [fuzz.WRatio, fuzz.QRatio,
                  fuzz.token_set_ratio, fuzz.token_sort_ratio,
                  fuzz.partial_token_set_ratio, fuzz.partial_token_sort_ratio,
                  fuzz.UWRatio, fuzz.UQRatio] \
            and processor == utils.full_process:
        processor = no_process

    # Only process the query once instead of for every choice
    if scorer in [fuzz.UWRatio, fuzz.UQRatio]:
        pre_processor = partial(utils.full_process, force_ascii=False)
        scorer = partial(scorer, full_process=False)
    elif scorer in [fuzz.WRatio, fuzz.QRatio,
                    fuzz.token_set_ratio, fuzz.token_sort_ratio,
                    fuzz.partial_token_set_ratio, fuzz.partial_token_sort_ratio]:
        pre_processor = partial(utils.full_process, force_ascii=True)
        scorer = partial(scorer, full_process=False)
    else:
        pre_processor = no_process
    processed_query = pre_processor(processed_query)

    try:
        # See if choices is a dictionary-like object.
        for key, choice in choices.items():
            processed = pre_processor(processor(choice))
            score = scorer(processed_query, processed)
            if score >= score_cutoff:
                yield (choice, score, key)
    except AttributeError:
        # It's a list; just iterate over it.
        for choice in choices:
            processed = pre_processor(processor(choice))
            score = scorer(processed_query, processed)
            if score >= score_cutoff:
                yield (choice, score)


def extract(query, choices, processor=default_processor, scorer=default_scorer, limit=5):
    """Select the best match in a list or dictionary of choices.

    Find best matches in a list or dictionary of choices, return a
    list of tuples containing the match and its score. If a dictionary
    is used, also returns the key for each match.

    Arguments:
        query: An object representing the thing we want to find.
        choices: An iterable or dictionary-like object containing choices
            to be matched against the query. Dictionary arguments of
            {key: value} pairs will attempt to match the query against
            each value.
        processor: Optional function of the form f(a) -> b, where a is the query or
            individual choice and b is the choice to be used in matching.

            This can be used to match against, say, the first element of
            a list:

            lambda x: x[0]

            Defaults to fuzzywuzzy.utils.full_process().
        scorer: Optional function for scoring matches between the query and
            an individual processed choice. This should be a function
            of the form f(query, choice) -> int.
            By default, fuzz.WRatio() is used and expects both query and
            choice to be strings.
        limit: Optional maximum for the number of elements returned. Defaults
            to 5.

    Returns:
        List of tuples containing the match and its score.

        If a list is used for choices, then the result will be 2-tuples.
        If a dictionary is used, then the result will be 3-tuples containing
        the key for each match.

        For example, searching for 'bird' in the dictionary

        {'bard': 'train', 'dog': 'man'}

        may return

        [('train', 22, 'bard'), ('man', 0, 'dog')]
    """
    sl = extractWithoutOrder(query, choices, processor, scorer)
    return heapq.nlargest(limit, sl, key=lambda i: i[1]) if limit is not None else \
        sorted(sl, key=lambda i: i[1], reverse=True)


def extractBests(query, choices, processor=default_processor, scorer=default_scorer, score_cutoff=0, limit=5):
    """Get a list of the best matches to a collection of choices.

    Convenience function for getting the choices with best scores.

    Args:
        query: A string to match against
        choices: A list or dictionary of choices, suitable for use with
            extract().
        processor: Optional function for transforming choices before matching.
            See extract().
        scorer: Scoring function for extract().
        score_cutoff: Optional argument for score threshold. No matches with
            a score less than this number will be returned. Defaults to 0.
        limit: Optional maximum for the number of elements returned. Defaults
            to 5.

    Returns: A a list of (match, score) tuples.
    """

    best_list = extractWithoutOrder(query, choices, processor, scorer, score_cutoff)
    return heapq.nlargest(limit, best_list, key=lambda i: i[1]) if limit is not None else \
        sorted(best_list, key=lambda i: i[1], reverse=True)


def extractOne(query, choices, processor=default_processor, scorer=default_scorer, score_cutoff=0):
    """Find the single best match above a score in a list of choices.

    This is a convenience method which returns the single best choice.
    See extract() for the full arguments list.

    Args:
        query: A string to match against
        choices: A list or dictionary of choices, suitable for use with
            extract().
        processor: Optional function for transforming choices before matching.
            See extract().
        scorer: Scoring function for extract().
        score_cutoff: Optional argument for score threshold. If the best
            match is found, but it is not greater than this number, then
            return None anyway ("not a good enough match").  Defaults to 0.

    Returns:
        A tuple containing a single match and its score, if a match
        was found that was above score_cutoff. Otherwise, returns None.
    """
    best_list = extractWithoutOrder(query, choices, processor, scorer, score_cutoff)
    try:
        return max(best_list, key=lambda i: i[1])
    except ValueError:
        return None


def dedupe(contains_dupes, threshold=70, scorer=fuzz.token_set_ratio):
    """This convenience function takes a list of strings containing duplicates and uses fuzzy matching to identify
    and remove duplicates. Specifically, it uses the process.extract to identify duplicates that
    score greater than a user defined threshold. Then, it looks for the longest item in the duplicate list
    since we assume this item contains the most entity information and returns that. It breaks string
    length ties on an alphabetical sort.

    Note: as the threshold DECREASES the number of duplicates that are found INCREASES. This means that the
        returned deduplicated list will likely be shorter. Raise the threshold for fuzzy_dedupe to be less
        sensitive.

    Args:
        contains_dupes: A list of strings that we would like to dedupe.
        threshold: the numerical value (0,100) point at which we expect to find duplicates.
            Defaults to 70 out of 100
        scorer: Optional function for scoring matches between the query and
            an individual processed choice. This should be a function
            of the form f(query, choice) -> int.
            By default, fuzz.token_set_ratio() is used and expects both query and
            choice to be strings.

    Returns:
        A deduplicated list. For example:

            In: contains_dupes = ['Frodo Baggin', 'Frodo Baggins', 'F. Baggins', 'Samwise G.', 'Gandalf', 'Bilbo Baggins']
            In: fuzzy_dedupe(contains_dupes)
            Out: ['Frodo Baggins', 'Samwise G.', 'Bilbo Baggins', 'Gandalf']
        """

    extractor = []

    # iterate over items in *contains_dupes*
    for item in contains_dupes:
        # return all duplicate matches found
        matches = extract(item, contains_dupes, limit=None, scorer=scorer)
        # filter matches based on the threshold
        filtered = [x for x in matches if x[1] > threshold]
        # if there is only 1 item in *filtered*, no duplicates were found so append to *extracted*
        if len(filtered) == 1:
            extractor.append(filtered[0][0])

        else:
            # alpha sort
            filtered = sorted(filtered, key=lambda x: x[0])
            # length sort
            filter_sort = sorted(filtered, key=lambda x: len(x[0]), reverse=True)
            # take first item as our 'canonical example'
            extractor.append(filter_sort[0][0])

    # uniquify *extractor* list
    keys = {}
    for e in extractor:
        keys[e] = 1
    extractor = keys.keys()

    # check that extractor differs from contain_dupes (e.g. duplicates were found)
    # if not, then return the original list
    if len(extractor) == len(contains_dupes):
        return contains_dupes
    else:
        return extractor


