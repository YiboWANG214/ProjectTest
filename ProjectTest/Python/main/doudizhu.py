'''
doudizhu/base.py
'''
''' Game-related base classes
'''
class Card:
    '''
    Card stores the suit and rank of a single card

    Note:
        The suit variable in a standard card game should be one of [S, H, D, C, BJ, RJ] meaning [Spades, Hearts, Diamonds, Clubs, Black Joker, Red Joker]
        Similarly the rank variable should be one of [A, 2, 3, 4, 5, 6, 7, 8, 9, T, J, Q, K]
    '''
    suit = None
    rank = None
    valid_suit = ['S', 'H', 'D', 'C', 'BJ', 'RJ']
    valid_rank = ['A', '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K']

    def __init__(self, suit, rank):
        ''' Initialize the suit and rank of a card

        Args:
            suit: string, suit of the card, should be one of valid_suit
            rank: string, rank of the card, should be one of valid_rank
        '''
        self.suit = suit
        self.rank = rank

    def __eq__(self, other):
        if isinstance(other, Card):
            return self.rank == other.rank and self.suit == other.suit
        else:
            # don't attempt to compare against unrelated types
            return NotImplemented

    def __hash__(self):
        suit_index = Card.valid_suit.index(self.suit)
        rank_index = Card.valid_rank.index(self.rank)
        return rank_index + 100 * suit_index

    def __str__(self):
        ''' Get string representation of a card.

        Returns:
            string: the combination of rank and suit of a card. Eg: AS, 5H, JD, 3C, ...
        '''
        return self.rank + self.suit

    def get_index(self):
        ''' Get index of a card.

        Returns:
            string: the combination of suit and rank of a card. Eg: 1S, 2H, AD, BJ, RJ...
        '''
        return self.suit+self.rank


'''
doudizhu/player.py
'''
# -*- coding: utf-8 -*-
''' Implement Doudizhu Player class
'''
import functools

from doudizhu.utils import get_gt_cards
from doudizhu.utils import cards2str, doudizhu_sort_card


class DoudizhuPlayer:
    ''' Player can store cards in the player's hand and the role,
    determine the actions can be made according to the rules,
    and can perfrom corresponding action
    '''
    def __init__(self, player_id, np_random):
        ''' Give the player an id in one game

        Args:
            player_id (int): the player_id of a player

        Notes:
            1. role: A player's temporary role in one game(landlord or peasant)
            2. played_cards: The cards played in one round
            3. hand: Initial cards
            4. _current_hand: The rest of the cards after playing some of them
        '''
        self.np_random = np_random
        self.player_id = player_id
        self.initial_hand = None
        self._current_hand = []
        self.role = ''
        self.played_cards = None
        self.singles = '3456789TJQKA2BR'

        #record cards removed from self._current_hand for each play()
        # and restore cards back to self._current_hand when play_back()
        self._recorded_played_cards = []

    @property
    def current_hand(self):
        return self._current_hand

    def set_current_hand(self, value):
        self._current_hand = value

    def get_state(self, public, others_hands, num_cards_left, actions):
        state = {}
        state['seen_cards'] = public['seen_cards']
        state['landlord'] = public['landlord']
        state['trace'] = public['trace'].copy()
        state['played_cards'] = public['played_cards']
        state['self'] = self.player_id
        state['current_hand'] = cards2str(self._current_hand)
        state['others_hand'] = others_hands
        state['num_cards_left'] = num_cards_left
        state['actions'] = actions

        return state

    def available_actions(self, greater_player=None, judger=None):
        ''' Get the actions can be made based on the rules

        Args:
            greater_player (DoudizhuPlayer object): player who played
        current biggest cards.
            judger (DoudizhuJudger object): object of DoudizhuJudger

        Returns:
            list: list of string of actions. Eg: ['pass', '8', '9', 'T', 'J']
        '''
        actions = []
        if greater_player is None or greater_player.player_id == self.player_id:
            actions = judger.get_playable_cards(self)
        else:
            actions = get_gt_cards(self, greater_player)
        return actions

    def play(self, action, greater_player=None):
        ''' Perfrom action

        Args:
            action (string): specific action
            greater_player (DoudizhuPlayer object): The player who played current biggest cards.

        Returns:
            object of DoudizhuPlayer: If there is a new greater_player, return it, if not, return None
        '''
        trans = {'B': 'BJ', 'R': 'RJ'}
        if action == 'pass':
            self._recorded_played_cards.append([])
            return greater_player
        else:
            removed_cards = []
            self.played_cards = action
            for play_card in action:
                if play_card in trans:
                    play_card = trans[play_card]
                for _, remain_card in enumerate(self._current_hand):
                    if remain_card.rank != '':
                        remain_card = remain_card.rank
                    else:
                        remain_card = remain_card.suit
                    if play_card == remain_card:
                        removed_cards.append(self.current_hand[_])
                        self._current_hand.remove(self._current_hand[_])
                        break
            self._recorded_played_cards.append(removed_cards)
            return self

    def play_back(self):
        ''' Restore recorded cards back to self._current_hand
        '''
        removed_cards = self._recorded_played_cards.pop()
        self._current_hand.extend(removed_cards)
        self._current_hand.sort(key=functools.cmp_to_key(doudizhu_sort_card))


'''
doudizhu/dealer.py
'''
# -*- coding: utf-8 -*-
''' Implement Doudizhu Dealer class
'''
import functools

from doudizhu import Card
from doudizhu.utils import cards2str, doudizhu_sort_card

def init_54_deck():
    ''' Initialize a standard deck of 52 cards, BJ and RJ

    Returns:
        (list): Alist of Card object
    '''
    suit_list = ['S', 'H', 'D', 'C']
    rank_list = ['A', '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K']
    res = [Card(suit, rank) for suit in suit_list for rank in rank_list]
    res.append(Card('BJ', ''))
    res.append(Card('RJ', ''))
    return res
    
class DoudizhuDealer:
    ''' Dealer will shuffle, deal cards, and determine players' roles
    '''
    def __init__(self, np_random):
        '''Give dealer the deck

        Notes:
            1. deck with 54 cards including black joker and red joker
        '''
        self.np_random = np_random
        self.deck = init_54_deck()
        self.deck.sort(key=functools.cmp_to_key(doudizhu_sort_card))
        self.landlord = None

    def shuffle(self):
        ''' Randomly shuffle the deck
        '''
        self.np_random.shuffle(self.deck)

    def deal_cards(self, players):
        ''' Deal cards to players

        Args:
            players (list): list of DoudizhuPlayer objects
        '''
        hand_num = (len(self.deck) - 3) // len(players)
        for index, player in enumerate(players):
            current_hand = self.deck[index*hand_num:(index+1)*hand_num]
            current_hand.sort(key=functools.cmp_to_key(doudizhu_sort_card))
            player.set_current_hand(current_hand)
            player.initial_hand = cards2str(player.current_hand)

    def determine_role(self, players):
        ''' Determine landlord and peasants according to players' hand

        Args:
            players (list): list of DoudizhuPlayer objects

        Returns:
            int: landlord's player_id
        '''
        # deal cards
        self.shuffle()
        self.deal_cards(players)
        players[0].role = 'landlord'
        self.landlord = players[0]
        players[1].role = 'peasant'
        players[2].role = 'peasant'
        #players[0].role = 'peasant'
        #self.landlord = players[0]

        ## determine 'landlord'
        #max_score = get_landlord_score(
        #    cards2str(self.landlord.current_hand))
        #for player in players[1:]:
        #    player.role = 'peasant'
        #    score = get_landlord_score(
        #        cards2str(player.current_hand))
        #    if score > max_score:
        #        max_score = score
        #        self.landlord = player
        #self.landlord.role = 'landlord'

        # give the 'landlord' the  three cards
        self.landlord.current_hand.extend(self.deck[-3:])
        self.landlord.current_hand.sort(key=functools.cmp_to_key(doudizhu_sort_card))
        self.landlord.initial_hand = cards2str(self.landlord.current_hand)
        return self.landlord.player_id


'''
doudizhu/judger.py
'''
# -*- coding: utf-8 -*-
''' Implement Doudizhu Judger class
'''
import numpy as np
import collections
from itertools import combinations
from bisect import bisect_left

from doudizhu.utils import CARD_RANK_STR, CARD_RANK_STR_INDEX
from doudizhu.utils import cards2str, contains_cards



class DoudizhuJudger:
    ''' Determine what cards a player can play
    '''
    @staticmethod
    def chain_indexes(indexes_list):
        ''' Find chains for solos, pairs and trios by using indexes_list

        Args:
            indexes_list: the indexes of cards those have the same count, the count could be 1, 2, or 3.

        Returns:
            list of tuples: [(start_index1, length1), (start_index1, length1), ...]

        '''
        chains = []
        prev_index = -100
        count = 0
        start = None
        for i in indexes_list:
            if (i[0] >= 12): #no chains for '2BR'
                break
            if (i[0] == prev_index + 1):
                count += 1
            else:
                if (count > 1):
                    chains.append((start, count))
                count = 1
                start = i[0]
            prev_index = i[0]
        if (count > 1):
            chains.append((start, count))
        return chains

    @classmethod
    def solo_attachments(cls, hands, chain_start, chain_length, size):
        ''' Find solo attachments for trio_chain_solo_x and four_two_solo

        Args:
            hands:
            chain_start: the index of start card of the trio_chain or trio or four
            chain_length: the size of the sequence of the chain, 1 for trio_solo or four_two_solo
            size: count of solos for the attachments

        Returns:
            list of tuples: [attachment1, attachment2, ...]
                            Each attachment has two elemnts,
                            the first one contains indexes of attached cards smaller than the index of chain_start,
                            the first one contains indexes of attached cards larger than the index of chain_start
        '''
        attachments = set()
        candidates = []
        prev_card = None
        same_card_count = 0
        for card in hands:
            #dont count those cards in the chain
            if (CARD_RANK_STR_INDEX[card] >= chain_start and CARD_RANK_STR_INDEX[card] < chain_start + chain_length):
                continue
            if (card == prev_card):
                #attachments can not have bomb
                if (same_card_count == 3):
                    continue
                #attachments can not have 3 same cards consecutive with the trio (except 3 cards of '222')
                elif (same_card_count == 2 and (CARD_RANK_STR_INDEX[card] == chain_start - 1 or CARD_RANK_STR_INDEX[card] == chain_start + chain_length) and card != '2'):
                    continue
                else:
                    same_card_count += 1
            else:
                prev_card = card
                same_card_count = 1
            candidates.append(CARD_RANK_STR_INDEX[card])
        for attachment in combinations(candidates, size):
            if (attachment[-1] == 14 and attachment[-2] == 13):
                continue
            i = bisect_left(attachment, chain_start)
            attachments.add((attachment[:i], attachment[i:]))
        return list(attachments)

    @classmethod
    def pair_attachments(cls, cards_count, chain_start, chain_length, size):
        ''' Find pair attachments for trio_chain_pair_x and four_two_pair

        Args:
            cards_count:
            chain_start: the index of start card of the trio_chain or trio or four
            chain_length: the size of the sequence of the chain, 1 for trio_pair or four_two_pair
            size: count of pairs for the attachments

        Returns:
            list of tuples: [attachment1, attachment2, ...]
                            Each attachment has two elemnts,
                            the first one contains indexes of attached cards smaller than the index of chain_start,
                            the first one contains indexes of attached cards larger than the index of chain_start
        '''
        attachments = set()
        candidates = []
        for i, _ in enumerate(cards_count):
            if (i >= chain_start and i < chain_start + chain_length):
                continue
            if (cards_count[i] == 2 or cards_count[i] == 3):
                candidates.append(i)
            elif (cards_count[i] == 4):
                candidates.append(i)
        for attachment in combinations(candidates, size):
            if (attachment[-1] == 14 and attachment[-2] == 13):
                continue
            i = bisect_left(attachment, chain_start)
            attachments.add((attachment[:i], attachment[i:]))
        return list(attachments)
        
    @staticmethod
    def playable_cards_from_hand(current_hand):
        ''' Get playable cards from hand

        Returns:
            set: set of string of playable cards
        '''
        cards_dict = collections.defaultdict(int)
        for card in current_hand:
            cards_dict[card] += 1
        cards_count = np.array([cards_dict[k] for k in CARD_RANK_STR])
        playable_cards = set()

        non_zero_indexes = np.argwhere(cards_count > 0)
        more_than_1_indexes = np.argwhere(cards_count > 1)
        more_than_2_indexes = np.argwhere(cards_count > 2)
        more_than_3_indexes = np.argwhere(cards_count > 3)
        #solo
        for i in non_zero_indexes:
            playable_cards.add(CARD_RANK_STR[i[0]])
        #pair
        for i in more_than_1_indexes:
            playable_cards.add(CARD_RANK_STR[i[0]] * 2)
        #bomb, four_two_solo, four_two_pair
        for i in more_than_3_indexes:
            cards = CARD_RANK_STR[i[0]] * 4
            playable_cards.add(cards)
            for left, right in DoudizhuJudger.solo_attachments(current_hand, i[0], 1, 2):
                pre_attached = ''
                for j in left:
                    pre_attached += CARD_RANK_STR[j]
                post_attached = ''
                for j in right:
                    post_attached += CARD_RANK_STR[j]
                playable_cards.add(pre_attached + cards + post_attached)
            for left, right in DoudizhuJudger.pair_attachments(cards_count, i[0], 1, 2):
                pre_attached = ''
                for j in left:
                    pre_attached += CARD_RANK_STR[j] * 2
                post_attached = ''
                for j in right:
                    post_attached += CARD_RANK_STR[j] * 2
                playable_cards.add(pre_attached + cards + post_attached)

        #solo_chain_5 -- #solo_chain_12
        solo_chain_indexes = DoudizhuJudger.chain_indexes(non_zero_indexes)
        for (start_index, length) in solo_chain_indexes:
            s, l = start_index, length
            while(l >= 5):
                cards = ''
                curr_index = s - 1
                curr_length = 0
                while (curr_length < l and curr_length < 12):
                    curr_index += 1
                    curr_length += 1
                    cards += CARD_RANK_STR[curr_index]
                    if (curr_length >= 5):
                        playable_cards.add(cards)
                l -= 1
                s += 1

        #pair_chain_3 -- #pair_chain_10
        pair_chain_indexes = DoudizhuJudger.chain_indexes(more_than_1_indexes)
        for (start_index, length) in pair_chain_indexes:
            s, l = start_index, length
            while(l >= 3):
                cards = ''
                curr_index = s - 1
                curr_length = 0
                while (curr_length < l and curr_length < 10):
                    curr_index += 1
                    curr_length += 1
                    cards += CARD_RANK_STR[curr_index] * 2
                    if (curr_length >= 3):
                        playable_cards.add(cards)
                l -= 1
                s += 1

        #trio, trio_solo and trio_pair
        for i in more_than_2_indexes:
            playable_cards.add(CARD_RANK_STR[i[0]] * 3)
            for j in non_zero_indexes:
                if (j < i):
                    playable_cards.add(CARD_RANK_STR[j[0]] + CARD_RANK_STR[i[0]] * 3)
                elif (j > i):
                    playable_cards.add(CARD_RANK_STR[i[0]] * 3 + CARD_RANK_STR[j[0]])
            for j in more_than_1_indexes:
                if (j < i):
                    playable_cards.add(CARD_RANK_STR[j[0]] * 2 + CARD_RANK_STR[i[0]] * 3)
                elif (j > i):
                    playable_cards.add(CARD_RANK_STR[i[0]] * 3 + CARD_RANK_STR[j[0]] * 2)

        #trio_solo, trio_pair, #trio -- trio_chain_2 -- trio_chain_6; trio_solo_chain_2 -- trio_solo_chain_5; trio_pair_chain_2 -- trio_pair_chain_4
        trio_chain_indexes = DoudizhuJudger.chain_indexes(more_than_2_indexes)
        for (start_index, length) in trio_chain_indexes:
            s, l = start_index, length
            while(l >= 2):
                cards = ''
                curr_index = s - 1
                curr_length = 0
                while (curr_length < l and curr_length < 6):
                    curr_index += 1
                    curr_length += 1
                    cards += CARD_RANK_STR[curr_index] * 3

                    #trio_chain_2 to trio_chain_6
                    if (curr_length >= 2 and curr_length <= 6):
                        playable_cards.add(cards)

                    #trio_solo_chain_2 to trio_solo_chain_5
                    if (curr_length >= 2 and curr_length <= 5):
                        for left, right in DoudizhuJudger.solo_attachments(current_hand, s, curr_length, curr_length):
                            pre_attached = ''
                            for j in left:
                                pre_attached += CARD_RANK_STR[j]
                            post_attached = ''
                            for j in right:
                                post_attached += CARD_RANK_STR[j]
                            playable_cards.add(pre_attached + cards + post_attached)

                    #trio_pair_chain2 -- trio_pair_chain_4
                    if (curr_length >= 2 and curr_length <= 4):
                        for left, right in DoudizhuJudger.pair_attachments(cards_count, s, curr_length, curr_length):
                            pre_attached = ''
                            for j in left:
                                pre_attached += CARD_RANK_STR[j] * 2
                            post_attached = ''
                            for j in right:
                                post_attached += CARD_RANK_STR[j] * 2
                            playable_cards.add(pre_attached + cards + post_attached)
                l -= 1
                s += 1
        #rocket
        if (cards_count[13] and cards_count[14]):
            playable_cards.add(CARD_RANK_STR[13] + CARD_RANK_STR[14])
        return playable_cards

    def __init__(self, players, np_random):
        ''' Initilize the Judger class for Dou Dizhu
        '''
        self.playable_cards = [set() for _ in range(3)]
        self._recorded_removed_playable_cards = [[] for _ in range(3)]
        for player in players:
            player_id = player.player_id
            current_hand = cards2str(player.current_hand)
            self.playable_cards[player_id] = self.playable_cards_from_hand(current_hand)

    def calc_playable_cards(self, player):
        ''' Recalculate all legal cards the player can play according to his
        current hand.

        Args:
            player (DoudizhuPlayer object): object of DoudizhuPlayer
            init_flag (boolean): For the first time, set it True to accelerate
              the preocess.

        Returns:
            list: list of string of playable cards
        '''
        removed_playable_cards = []

        player_id = player.player_id
        current_hand = cards2str(player.current_hand)
        missed = None
        for single in player.singles:
            if single not in current_hand:
                missed = single
                break

        playable_cards = self.playable_cards[player_id].copy()

        if missed is not None:
            position = player.singles.find(missed)
            player.singles = player.singles[position+1:]
            for cards in playable_cards:
                if missed in cards or (not contains_cards(current_hand, cards)):
                    removed_playable_cards.append(cards)
                    self.playable_cards[player_id].remove(cards)
        else:
            for cards in playable_cards:
                if not contains_cards(current_hand, cards):
                    #del self.playable_cards[player_id][cards]
                    removed_playable_cards.append(cards)
                    self.playable_cards[player_id].remove(cards)
        self._recorded_removed_playable_cards[player_id].append(removed_playable_cards)
        return self.playable_cards[player_id]

    def restore_playable_cards(self, player_id):
        ''' restore playable_cards for judger for game.step_back().

        Args:
            player_id: The id of the player whose playable_cards need to be restored
        '''
        removed_playable_cards = self._recorded_removed_playable_cards[player_id].pop()
        self.playable_cards[player_id].update(removed_playable_cards)

    def get_playable_cards(self, player):
        ''' Provide all legal cards the player can play according to his
        current hand.

        Args:
            player (DoudizhuPlayer object): object of DoudizhuPlayer
            init_flag (boolean): For the first time, set it True to accelerate
              the preocess.

        Returns:
            list: list of string of playable cards
        '''
        return self.playable_cards[player.player_id]


    @staticmethod
    def judge_game(players, player_id):
        ''' Judge whether the game is over

        Args:
            players (list): list of DoudizhuPlayer objects
            player_id (int): integer of player's id

        Returns:
            (bool): True if the game is over
        '''
        player = players[player_id]
        if not player.current_hand:
            return True
        return False

    @staticmethod
    def judge_payoffs(landlord_id, winner_id):
        payoffs = np.array([0, 0, 0])
        if winner_id == landlord_id:
            payoffs[landlord_id] = 1
        else:
            for index, _ in enumerate(payoffs):
                if index != landlord_id:
                    payoffs[index] = 1
        return payoffs


'''
doudizhu/game.py
'''
# -*- coding: utf-8 -*-
''' Implement Doudizhu Game class
'''
import functools
from heapq import merge
import numpy as np

from doudizhu.utils import cards2str, doudizhu_sort_card, CARD_RANK_STR
from doudizhu import Player
from doudizhu import Round
from doudizhu import Judger


class DoudizhuGame:
    ''' Provide game APIs for env to run doudizhu and get corresponding state
    information.
    '''
    def __init__(self, allow_step_back=False):
        self.allow_step_back = allow_step_back
        self.np_random = np.random.RandomState()
        self.num_players = 3

    def init_game(self):
        ''' Initialize players and state.

        Returns:
            dict: first state in one game
            int: current player's id
        '''
        # initialize public variables
        self.winner_id = None
        self.history = []

        # initialize players
        self.players = [Player(num, self.np_random)
                        for num in range(self.num_players)]

        # initialize round to deal cards and determine landlord
        self.played_cards = [np.zeros((len(CARD_RANK_STR), ), dtype=np.int32)
                                for _ in range(self.num_players)]
        self.round = Round(self.np_random, self.played_cards)
        self.round.initiate(self.players)

        # initialize judger
        self.judger = Judger(self.players, self.np_random)

        # get state of first player
        player_id = self.round.current_player
        self.state = self.get_state(player_id)

        return self.state, player_id

    def step(self, action):
        ''' Perform one draw of the game

        Args:
            action (str): specific action of doudizhu. Eg: '33344'

        Returns:
            dict: next player's state
            int: next player's id
        '''
        if self.allow_step_back:
            # TODO: don't record game.round, game.players, game.judger if allow_step_back not set
            pass

        # perfrom action
        player = self.players[self.round.current_player]
        self.round.proceed_round(player, action)
        if (action != 'pass'):
            self.judger.calc_playable_cards(player)
        if self.judger.judge_game(self.players, self.round.current_player):
            self.winner_id = self.round.current_player
        next_id = (player.player_id+1) % len(self.players)
        self.round.current_player = next_id

        # get next state
        state = self.get_state(next_id)
        self.state = state

        return state, next_id

    def step_back(self):
        ''' Return to the previous state of the game

        Returns:
            (bool): True if the game steps back successfully
        '''
        if not self.round.trace:
            return False

        #winner_id will be always None no matter step_back from any case
        self.winner_id = None

        #reverse round
        player_id, cards = self.round.step_back(self.players)

        #reverse player
        if (cards != 'pass'):
            self.players[player_id].played_cards = self.round.find_last_played_cards_in_trace(player_id)
        self.players[player_id].play_back()

        #reverse judger.played_cards if needed
        if (cards != 'pass'):
            self.judger.restore_playable_cards(player_id)

        self.state = self.get_state(self.round.current_player)
        return True

    def get_state(self, player_id):
        ''' Return player's state

        Args:
            player_id (int): player id

        Returns:
            (dict): The state of the player
        '''
        player = self.players[player_id]
        others_hands = self._get_others_current_hand(player)
        num_cards_left = [len(self.players[i].current_hand) for i in range(self.num_players)]
        if self.is_over():
            actions = []
        else:
            actions = list(player.available_actions(self.round.greater_player, self.judger))
        state = player.get_state(self.round.public, others_hands, num_cards_left, actions)

        return state

    @staticmethod
    def get_num_actions():
        ''' Return the total number of abstract acitons

        Returns:
            int: the total number of abstract actions of doudizhu
        '''
        return 27472

    def get_player_id(self):
        ''' Return current player's id

        Returns:
            int: current player's id
        '''
        return self.round.current_player

    def get_num_players(self):
        ''' Return the number of players in doudizhu

        Returns:
            int: the number of players in doudizhu
        '''
        return self.num_players

    def is_over(self):
        ''' Judge whether a game is over

        Returns:
            Bool: True(over) / False(not over)
        '''
        if self.winner_id is None:
            return False
        return True

    def _get_others_current_hand(self, player):
        player_up = self.players[(player.player_id+1) % len(self.players)]
        player_down = self.players[(player.player_id-1) % len(self.players)]
        others_hand = merge(player_up.current_hand, player_down.current_hand, key=functools.cmp_to_key(doudizhu_sort_card))
        return cards2str(others_hand)


'''
doudizhu/utils.py
'''
''' Doudizhu utils
'''
import os
import json
from collections import OrderedDict
import threading
import collections

# import rlcard

# Read required docs
ROOT_PATH = '.'

# if not os.path.isfile(os.path.join(ROOT_PATH, '/jsondata/action_space.txt')) \
#         or not os.path.isfile(os.path.join(ROOT_PATH, '/jsondata/card_type.json')) \
#         or not os.path.isfile(os.path.join(ROOT_PATH, '/jsondata/type_card.json')):
#     import zipfile
#     with zipfile.ZipFile(os.path.join(ROOT_PATH, 'jsondata.zip'),"r") as zip_ref:
#         zip_ref.extractall(os.path.join(ROOT_PATH, '/'))

# Action space
action_space_path = os.path.join(ROOT_PATH, './jsondata/action_space.txt')

with open(action_space_path, 'r') as f:
    ID_2_ACTION = f.readline().strip().split()
    ACTION_2_ID = {}
    for i, action in enumerate(ID_2_ACTION):
        ACTION_2_ID[action] = i

# a map of card to its type. Also return both dict and list to accelerate
card_type_path = os.path.join(ROOT_PATH, './jsondata/card_type.json')
with open(card_type_path, 'r') as f:
    data = json.load(f, object_pairs_hook=OrderedDict)
    CARD_TYPE = (data, list(data), set(data))

# a map of type to its cards
type_card_path = os.path.join(ROOT_PATH, './jsondata/type_card.json')
with open(type_card_path, 'r') as f:
    TYPE_CARD = json.load(f, object_pairs_hook=OrderedDict)

# rank list of solo character of cards
CARD_RANK_STR = ['3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K',
                 'A', '2', 'B', 'R']
CARD_RANK_STR_INDEX = {'3': 0, '4': 1, '5': 2, '6': 3, '7': 4,
            '8': 5, '9': 6, 'T': 7, 'J': 8, 'Q': 9,
            'K': 10, 'A': 11, '2': 12, 'B': 13, 'R': 14}
# rank list
CARD_RANK = ['3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K',
             'A', '2', 'BJ', 'RJ']

INDEX = {'3': 0, '4': 1, '5': 2, '6': 3, '7': 4,
         '8': 5, '9': 6, 'T': 7, 'J': 8, 'Q': 9,
         'K': 10, 'A': 11, '2': 12, 'B': 13, 'R': 14}
INDEX = OrderedDict(sorted(INDEX.items(), key=lambda t: t[1]))


def doudizhu_sort_str(card_1, card_2):
    ''' Compare the rank of two cards of str representation

    Args:
        card_1 (str): str representation of solo card
        card_2 (str): str representation of solo card

    Returns:
        int: 1(card_1 > card_2) / 0(card_1 = card2) / -1(card_1 < card_2)
    '''
    key_1 = CARD_RANK_STR.index(card_1)
    key_2 = CARD_RANK_STR.index(card_2)
    if key_1 > key_2:
        return 1
    if key_1 < key_2:
        return -1
    return 0


def doudizhu_sort_card(card_1, card_2):
    ''' Compare the rank of two cards of Card object

    Args:
        card_1 (object): object of Card
        card_2 (object): object of card
    '''
    key = []
    for card in [card_1, card_2]:
        if card.rank == '':
            key.append(CARD_RANK.index(card.suit))
        else:
            key.append(CARD_RANK.index(card.rank))
    if key[0] > key[1]:
        return 1
    if key[0] < key[1]:
        return -1
    return 0


def get_landlord_score(current_hand):
    ''' Roughly judge the quality of the hand, and provide a score as basis to
    bid landlord.

    Args:
        current_hand (str): string of cards. Eg: '56888TTQKKKAA222R'

    Returns:
        int: score
    '''
    score_map = {'A': 1, '2': 2, 'B': 3, 'R': 4}
    score = 0
    # rocket
    if current_hand[-2:] == 'BR':
        score += 8
        current_hand = current_hand[:-2]
    length = len(current_hand)
    i = 0
    while i < length:
        # bomb
        if i <= (length - 4) and current_hand[i] == current_hand[i+3]:
            score += 6
            i += 4
            continue
        # 2, Black Joker, Red Joker
        if current_hand[i] in score_map:
            score += score_map[current_hand[i]]
        i += 1
    return score

def cards2str_with_suit(cards):
    ''' Get the corresponding string representation of cards with suit

    Args:
        cards (list): list of Card objects

    Returns:
        string: string representation of cards
    '''
    return ' '.join([card.suit+card.rank for card in cards])

def cards2str(cards):
    ''' Get the corresponding string representation of cards

    Args:
        cards (list): list of Card objects

    Returns:
        string: string representation of cards
    '''
    response = ''
    for card in cards:
        if card.rank == '':
            response += card.suit[0]
        else:
            response += card.rank
    return response

class LocalObjs(threading.local):
    def __init__(self):
        self.cached_candidate_cards = None
_local_objs = LocalObjs()

def contains_cards(candidate, target):
    ''' Check if cards of candidate contains cards of target.

    Args:
        candidate (string): A string representing the cards of candidate
        target (string): A string representing the number of cards of target

    Returns:
        boolean
    '''
    # In normal cases, most continuous calls of this function
    #   will test different targets against the same candidate.
    # So the cached counts of each card in candidate can speed up
    #   the comparison for following tests if candidate keeps the same.
    if not _local_objs.cached_candidate_cards or _local_objs.cached_candidate_cards != candidate:
        _local_objs.cached_candidate_cards = candidate
        cards_dict = collections.defaultdict(int)
        for card in candidate:
            cards_dict[card] += 1
        _local_objs.cached_candidate_cards_dict = cards_dict
    cards_dict = _local_objs.cached_candidate_cards_dict
    if (target == ''):
        return True
    curr_card = target[0]
    curr_count = 1
    for card in target[1:]:
        if (card != curr_card):
            if (cards_dict[curr_card] < curr_count):
                return False
            curr_card = card
            curr_count = 1
        else:
            curr_count += 1
    if (cards_dict[curr_card] < curr_count):
        return False
    return True

def encode_cards(plane, cards):
    ''' Encode cards and represerve it into plane.

    Args:
        cards (list or str): list or str of cards, every entry is a
    character of solo representation of card
    '''
    if not cards:
        return None
    layer = 1
    if len(cards) == 1:
        rank = CARD_RANK_STR.index(cards[0])
        plane[layer][rank] = 1
        plane[0][rank] = 0
    else:
        for index, card in enumerate(cards):
            if index == 0:
                continue
            if card == cards[index-1]:
                layer += 1
            else:
                rank = CARD_RANK_STR.index(cards[index-1])
                plane[layer][rank] = 1
                layer = 1
                plane[0][rank] = 0
        rank = CARD_RANK_STR.index(cards[-1])
        plane[layer][rank] = 1
        plane[0][rank] = 0


def get_gt_cards(player, greater_player):
    ''' Provide player's cards which are greater than the ones played by
    previous player in one round

    Args:
        player (DoudizhuPlayer object): the player waiting to play cards
        greater_player (DoudizhuPlayer object): the player who played current biggest cards.

    Returns:
        list: list of string of greater cards

    Note:
        1. return value contains 'pass'
    '''
    # add 'pass' to legal actions
    gt_cards = ['pass']
    current_hand = cards2str(player.current_hand)
    target_cards = greater_player.played_cards
    target_types = CARD_TYPE[0][target_cards]
    type_dict = {}
    for card_type, weight in target_types:
        if card_type not in type_dict:
            type_dict[card_type] = weight
    if 'rocket' in type_dict:
        return gt_cards
    type_dict['rocket'] = -1
    if 'bomb' not in type_dict:
        type_dict['bomb'] = -1
    for card_type, weight in type_dict.items():
        candidate = TYPE_CARD[card_type]
        for can_weight, cards_list in candidate.items():
            if int(can_weight) > int(weight):
                for cards in cards_list:
                    # TODO: improve efficiency
                    if cards not in gt_cards and contains_cards(current_hand, cards):
                        # if self.contains_cards(current_hand, cards):
                        gt_cards.append(cards)
    return gt_cards


'''
doudizhu/__init__.py
'''
from doudizhu.base import Card as Card
from doudizhu.dealer import DoudizhuDealer as Dealer
from doudizhu.judger import DoudizhuJudger as Judger
from doudizhu.player import DoudizhuPlayer as Player
from doudizhu.round import DoudizhuRound as Round
from doudizhu.game import DoudizhuGame as Game

'''
doudizhu/round.py
'''
# -*- coding: utf-8 -*-
''' Implement Doudizhu Round class
'''

import functools
import numpy as np

from doudizhu import Dealer
from doudizhu.utils import cards2str, doudizhu_sort_card
from doudizhu.utils import CARD_RANK_STR, CARD_RANK_STR_INDEX


class DoudizhuRound:
    ''' Round can call other Classes' functions to keep the game running
    '''
    def __init__(self, np_random, played_cards):
        self.np_random = np_random
        self.played_cards = played_cards
        self.trace = []

        self.greater_player = None
        self.dealer = Dealer(self.np_random)
        self.deck_str = cards2str(self.dealer.deck)

    def initiate(self, players):
        ''' Call dealer to deal cards and bid landlord.

        Args:
            players (list): list of DoudizhuPlayer objects
        '''
        landlord_id = self.dealer.determine_role(players)
        seen_cards = self.dealer.deck[-3:]
        seen_cards.sort(key=functools.cmp_to_key(doudizhu_sort_card))
        self.seen_cards = cards2str(seen_cards)
        self.landlord_id = landlord_id
        self.current_player = landlord_id
        self.public = {'deck': self.deck_str, 'seen_cards': self.seen_cards,
                       'landlord': self.landlord_id, 'trace': self.trace,
                       'played_cards': ['' for _ in range(len(players))]}

    @staticmethod
    def cards_ndarray_to_str(ndarray_cards):
        result = []
        for cards in ndarray_cards:
            _result = []
            for i, _ in enumerate(cards):
                if cards[i] != 0:
                    _result.extend([CARD_RANK_STR[i]] * cards[i])
            result.append(''.join(_result))
        return result

    def update_public(self, action):
        ''' Update public trace and played cards

        Args:
            action(str): string of legal specific action
        '''
        self.trace.append((self.current_player, action))
        if action != 'pass':
            for c in action:
                self.played_cards[self.current_player][CARD_RANK_STR_INDEX[c]] += 1
                if self.current_player == 0 and c in self.seen_cards:
                    self.seen_cards = self.seen_cards.replace(c, '') 
                    self.public['seen_cards'] = self.seen_cards
            self.public['played_cards'] = self.cards_ndarray_to_str(self.played_cards)

    def proceed_round(self, player, action):
        ''' Call other Classes's functions to keep one round running

        Args:
            player (object): object of DoudizhuPlayer
            action (str): string of legal specific action

        Returns:
            object of DoudizhuPlayer: player who played current biggest cards.
        '''
        self.update_public(action)
        self.greater_player = player.play(action, self.greater_player)
        return self.greater_player

    def step_back(self, players):
        ''' Reverse the last action

        Args:
            players (list): list of DoudizhuPlayer objects
        Returns:
            The last player id and the cards played
        '''
        player_id, cards = self.trace.pop()
        self.current_player = player_id
        if (cards != 'pass'):
            for card in cards:
                # self.played_cards.remove(card)
                self.played_cards[player_id][CARD_RANK_STR_INDEX[card]] -= 1
            self.public['played_cards'] = self.cards_ndarray_to_str(self.played_cards)
        greater_player_id = self.find_last_greater_player_id_in_trace()
        if (greater_player_id is not None):
            self.greater_player = players[greater_player_id]
        else:
            self.greater_player = None
        return player_id, cards

    def find_last_greater_player_id_in_trace(self):
        ''' Find the last greater_player's id in trace

        Returns:
            The last greater_player's id in trace
        '''
        for i in range(len(self.trace) - 1, -1, -1):
            _id, action = self.trace[i]
            if (action != 'pass'):
                return _id
        return None

    def find_last_played_cards_in_trace(self, player_id):
        ''' Find the player_id's last played_cards in trace

        Returns:
            The player_id's last played_cards in trace
        '''
        for i in range(len(self.trace) - 1, -1, -1):
            _id, action = self.trace[i]
            if (_id == player_id and action != 'pass'):
                return action
        return None


