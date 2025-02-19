'''
mahjong/player.py
'''

class MahjongPlayer:

    def __init__(self, player_id, np_random):
        ''' Initilize a player.

        Args:
            player_id (int): The id of the player
        '''
        self.np_random = np_random
        self.player_id = player_id
        self.hand = []
        self.pile = []

    def get_player_id(self):
        ''' Return the id of the player
        '''

        return self.player_id

    def print_hand(self):
        ''' Print the cards in hand in string.
        '''
        print([c.get_str() for c in self.hand])

    def print_pile(self):
        ''' Print the cards in pile of the player in string.
        '''
        print([[c.get_str() for c in s]for s in self.pile])

    def play_card(self, dealer, card):
        ''' Play one card
        Args:
            dealer (object): Dealer
            Card (object): The card to be play.
        '''
        card = self.hand.pop(self.hand.index(card))
        dealer.table.append(card)

    def chow(self, dealer, cards):
        ''' Perform Chow
        Args:
            dealer (object): Dealer
            Cards (object): The cards to be Chow.
        '''
        last_card = dealer.table.pop(-1)
        for card in cards:
            if card in self.hand and card != last_card:
                self.hand.pop(self.hand.index(card))
        self.pile.append(cards)

    def gong(self, dealer, cards):
        ''' Perform Gong
        Args:
            dealer (object): Dealer
            Cards (object): The cards to be Gong.
        '''
        for card in cards:
            if card in self.hand:
                self.hand.pop(self.hand.index(card))
        self.pile.append(cards)

    def pong(self, dealer, cards):
        ''' Perform Pong
        Args:
            dealer (object): Dealer
            Cards (object): The cards to be Pong.
        '''
        for card in cards:
            if card in self.hand:
                self.hand.pop(self.hand.index(card))
        self.pile.append(cards)


'''
mahjong/dealer.py
'''
from utils import init_deck


class MahjongDealer:
    ''' Initialize a mahjong dealer class
    '''
    def __init__(self, np_random):
        self.np_random = np_random
        self.deck = init_deck()
        self.shuffle()
        self.table = []

    def shuffle(self):
        ''' Shuffle the deck
        '''
        self.np_random.shuffle(self.deck)

    def deal_cards(self, player, num):
        ''' Deal some cards from deck to one player

        Args:
            player (object): The object of DoudizhuPlayer
            num (int): The number of cards to be dealed
        '''
        for _ in range(num):
            player.hand.append(self.deck.pop())


## For test


'''
mahjong/card.py
'''

class MahjongCard:

    info = {'type':  ['dots', 'bamboo', 'characters', 'dragons', 'winds'],
            'trait': ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'green', 'red', 'white', 'east', 'west', 'north', 'south']
            }

    def __init__(self, card_type, trait):
        ''' Initialize the class of MahjongCard

        Args:
            card_type (str): The type of card
            trait (str): The trait of card
        '''
        self.type = card_type
        self.trait = trait
        self.index_num = 0

    def get_str(self):
        ''' Get the string representation of card

        Return:
            (str): The string of card's color and trait
        '''
        return self.type+ '-'+ self.trait

    def set_index_num(self, index_num):

        self.index_num = index_num
        



'''
mahjong/judger.py
'''
# -*- coding: utf-8 -*-
''' Implement Mahjong Judger class
'''
from collections import defaultdict
import numpy as np

class MahjongJudger:
    ''' Determine what cards a player can play
    '''

    def __init__(self, np_random):
        ''' Initilize the Judger class for Mahjong
        '''
        self.np_random = np_random

    @staticmethod
    def judge_pong_gong(dealer, players, last_player):
        ''' Judge which player has pong/gong
        Args:
            dealer (object): The dealer object.
            players (list): List of all players
            last_player (int): The player id of last player

        '''
        last_card = dealer.table[-1]
        last_card_str = last_card.get_str()
        #last_card_value = last_card_str.split("-")[-1]
        #last_card_type = last_card_str.split("-")[0]
        for player in players:
            hand = [card.get_str() for card in player.hand]
            hand_dict = defaultdict(list)
            for card in hand:
                hand_dict[card.split("-")[0]].append(card.split("-")[1])
            #pile = player.pile
            # check gong
            if hand.count(last_card_str) == 3 and last_player != player.player_id:
                return 'gong', player, [last_card]*4
            # check pong
            if hand.count(last_card_str) == 2 and last_player != player.player_id:
                return 'pong', player, [last_card]*3
        return False, None, None

    def judge_chow(self, dealer, players, last_player):
        ''' Judge which player has chow
        Args:
            dealer (object): The dealer object.
            players (list): List of all players
            last_player (int): The player id of last player
        '''

        last_card = dealer.table[-1]
        last_card_str = last_card.get_str()
        last_card_type = last_card_str.split("-")[0]
        last_card_index = last_card.index_num
        for player in players:
            if last_card_type != "dragons" and last_card_type != "winds" and last_player == player.get_player_id() - 1:
                # Create 9 dimensional vector where each dimension represent a specific card with the type same as last_card_type
                # Numbers in each dimension represent how many of that card the player has it in hand
                # If the last_card_type is 'characters' for example, and the player has cards: characters_3, characters_6, characters_3,
                # The hand_list vector looks like: [0,0,2,0,0,1,0,0,0]
                hand_list = np.zeros(9)

                for card in player.hand:
                    if card.get_str().split("-")[0] == last_card_type:
                        hand_list[card.index_num] = hand_list[card.index_num]+1

                #pile = player.pile
                #check chow
                test_cases = []
                if last_card_index == 0:
                    if hand_list[last_card_index+1] > 0 and hand_list[last_card_index+2] > 0:
                        test_cases.append([last_card_index+1, last_card_index+2])
                elif last_card_index < 9:
                    if hand_list[last_card_index-2] > 0 and hand_list[last_card_index-1] > 0:
                        test_cases.append([last_card_index-2, last_card_index-1])
                else:
                    if hand_list[last_card_index-1] > 0 and hand_list[last_card_index+1] > 0:
                        test_cases.append([last_card_index-1, last_card_index+1])

                if not test_cases:
                    continue        

                for l in test_cases:
                    cards = []
                    for i in l:
                        for card in player.hand:
                            if card.index_num == i and card.get_str().split("-")[0] == last_card_type:
                                cards.append(card)
                                break
                    cards.append(last_card)
                    return 'chow', player, cards
        return False, None, None

    def judge_game(self, game):
        ''' Judge which player has win the game
        Args:
            dealer (object): The dealer object.
            players (list): List of all players
            last_player (int): The player id of last player
        '''
        players_val = []
        win_player = -1
        for player in game.players:
            win, val = self.judge_hu(player)
            players_val.append(val)
            if win:
                win_player = player.player_id
        if win_player != -1 or len(game.dealer.deck) == 0:
            return True, win_player, players_val
        else:
            #player_id = players_val.index(max(players_val))
            return False, win_player, players_val

    def judge_hu(self, player):
        ''' Judge whether the player has win the game
        Args:
            player (object): Target player

        Return:
            Result (bool): Win or not
            Maximum_score (int): Set count score of the player
        '''
        set_count = 0
        hand = [card.get_str() for card in player.hand]
        count_dict = {card: hand.count(card) for card in hand}
        set_count = len(player.pile)
        if set_count >= 4:
            return True, set_count
        used = []
        maximum = 0
        for each in count_dict:
            if each in used:
                continue
            tmp_set_count = 0
            tmp_hand = hand.copy()
            if count_dict[each] == 2:
                for _ in range(count_dict[each]):
                    tmp_hand.pop(tmp_hand.index(each))
                tmp_set_count, _set = self.cal_set(tmp_hand)
                used.extend(_set)
                if tmp_set_count + set_count > maximum:
                    maximum = tmp_set_count + set_count
                if tmp_set_count + set_count >= 4:
                    #print(player.get_player_id(), sorted([card.get_str() for card in player.hand]))
                    #print([[c.get_str() for c in s] for s in player.pile])
                    #print(len(player.hand), sum([len(s) for s in player.pile]))
                    #exit()
                    return True, maximum
        return False, maximum

    @staticmethod
    def check_consecutive(_list):
        ''' Check if list is consecutive
        Args:
            _list (list): The target list

        Return:
            Result (bool): consecutive or not
        '''
        l = list(map(int, _list))
        if sorted(l) == list(range(min(l), max(l)+1)):
            return True
        return False

    def cal_set(self, cards):
        ''' Calculate the set for given cards
        Args:
            Cards (list): List of cards.

        Return:
            Set_count (int):
            Sets (list): List of cards that has been pop from user's hand
        '''
        tmp_cards = cards.copy()
        sets = []
        set_count = 0
        _dict = {card: tmp_cards.count(card) for card in tmp_cards}
        # check pong/gang
        for each in _dict:
            if _dict[each] == 3 or _dict[each] == 4:
                set_count += 1
                for _ in range(_dict[each]):
                    tmp_cards.pop(tmp_cards.index(each))

        # get all of the traits of each type in hand (except dragons and winds)
        _dict_by_type = defaultdict(list)
        for card in tmp_cards:
            _type = card.split("-")[0]
            _trait = card.split("-")[1]
            if _type == 'dragons' or _type == 'winds':
                continue
            else:
                _dict_by_type[_type].append(_trait)
        for _type in _dict_by_type.keys():
            values = sorted(_dict_by_type[_type])
            if len(values) > 2:
                for index, _ in enumerate(values):
                    if index == 0:
                        test_case = [values[index], values[index+1], values[index+2]]
                    elif index == len(values)-1:
                        test_case = [values[index-2], values[index-1], values[index]]
                    else:
                        test_case = [values[index-1], values[index], values[index+1]]
                    if self.check_consecutive(test_case):
                        set_count += 1
                        for each in test_case:
                            values.pop(values.index(each))
                            c = _type+"-"+str(each)
                            sets.append(c)
                            if c in tmp_cards:
                                tmp_cards.pop(tmp_cards.index(c))
        return set_count, sets



'''
mahjong/game.py
'''
import numpy as np
from copy import deepcopy

from mahjong import Dealer
from mahjong import Player
from mahjong import Round
from mahjong import Judger

class MahjongGame:

    def __init__(self, allow_step_back=False):
        '''Initialize the class MajongGame
        '''
        self.allow_step_back = allow_step_back
        self.np_random = np.random.RandomState()
        self.num_players = 4

    def init_game(self):
        ''' Initialilze the game of Mahjong

        This version supports two-player Mahjong

        Returns:
            (tuple): Tuple containing:

                (dict): The first state of the game
                (int): Current player's id
        '''
        # Initialize a dealer that can deal cards
        self.dealer = Dealer(self.np_random)

        # Initialize four players to play the game
        self.players = [Player(i, self.np_random) for i in range(self.num_players)]

        self.judger = Judger(self.np_random)
        self.round = Round(self.judger, self.dealer, self.num_players, self.np_random)

        # Deal 13 cards to each player to prepare for the game
        for player in self.players:
            self.dealer.deal_cards(player, 13)

        # Save the hisory for stepping back to the last state.
        self.history = []

        self.dealer.deal_cards(self.players[self.round.current_player], 1)
        state = self.get_state(self.round.current_player)
        self.cur_state = state
        return state, self.round.current_player

    def step(self, action):
        ''' Get the next state

        Args:
            action (str): a specific action. (call, raise, fold, or check)

        Returns:
            (tuple): Tuple containing:

                (dict): next player's state
                (int): next plater's id
        '''
        # First snapshot the current state
        if self.allow_step_back:
            hist_dealer = deepcopy(self.dealer)
            hist_round = deepcopy(self.round)
            hist_players = deepcopy(self.players)
            self.history.append((hist_dealer, hist_players, hist_round))
        self.round.proceed_round(self.players, action)
        state = self.get_state(self.round.current_player)
        self.cur_state = state
        return state, self.round.current_player

    def step_back(self):
        ''' Return to the previous state of the game

        Returns:
            (bool): True if the game steps back successfully
        '''
        if not self.history:
            return False
        self.dealer, self.players, self.round = self.history.pop()
        return True

    def get_state(self, player_id):
        ''' Return player's state

        Args:
            player_id (int): player id

        Returns:
            (dict): The state of the player
        '''
        state = self.round.get_state(self.players, player_id)
        return state

    @staticmethod
    def get_legal_actions(state):
        ''' Return the legal actions for current player

        Returns:
            (list): A list of legal actions
        '''
        if state['valid_act'] == ['play']:
            state['valid_act'] = state['action_cards']
            return state['action_cards']
        else:
            return state['valid_act']

    @staticmethod
    def get_num_actions():
        ''' Return the number of applicable actions

        Returns:
            (int): The number of actions. There are 4 actions (call, raise, check and fold)
        '''
        return 38

    def get_num_players(self):
        ''' return the number of players in Mahjong

        returns:
            (int): the number of players in the game
        '''
        return self.num_players

    def get_player_id(self):
        ''' return the id of current player in Mahjong

        returns:
            (int): the number of players in the game
        '''
        return self.round.current_player

    def is_over(self):
        ''' Check if the game is over

        Returns:
            (boolean): True if the game is over
        '''
        win, player, _ = self.judger.judge_game(self)
        #pile =[sorted([c.get_str() for c in s ]) for s in self.players[player].pile if self.players[player].pile != None]
        #cards = sorted([c.get_str() for c in self.players[player].hand])
        #count = len(cards) + sum([len(p) for p in pile])
        self.winner = player
        #print(win, player, players_val)
        #print(win, self.round.current_player, player, cards, pile, count)
        return win


'''
mahjong/utils.py
'''
import numpy as np
from card import MahjongCard as Card


card_encoding_dict = {}
num = 0
for _type in ['bamboo', 'characters', 'dots']:
    for _trait in ['1', '2', '3', '4', '5', '6', '7', '8', '9']:
        card = _type+"-"+_trait
        card_encoding_dict[card] = num
        num += 1
for _trait in ['green', 'red', 'white']:
    card = 'dragons-'+_trait
    card_encoding_dict[card] = num
    num += 1

for _trait in ['east', 'west', 'north', 'south']:
    card = 'winds-'+_trait
    card_encoding_dict[card] = num
    num += 1
card_encoding_dict['pong'] = num
card_encoding_dict['chow'] = num + 1
card_encoding_dict['gong'] = num + 2
card_encoding_dict['stand'] = num + 3

card_decoding_dict = {card_encoding_dict[key]: key for key in card_encoding_dict.keys()}

def init_deck():
    deck = []
    info = Card.info
    for _type in info['type']:
        index_num = 0
        if _type != 'dragons' and _type != 'winds':
            for _trait in info['trait'][:9]:
                card = Card(_type, _trait)
                card.set_index_num(index_num)
                index_num = index_num + 1
                deck.append(card)
        elif _type == 'dragons':
            for _trait in info['trait'][9:12]:
                card = Card(_type, _trait)
                card.set_index_num(index_num)
                index_num = index_num + 1
                deck.append(card)
        else:
            for _trait in info['trait'][12:]:
                card = Card(_type, _trait)
                card.set_index_num(index_num)
                index_num = index_num + 1
                deck.append(card)
    deck = deck * 4
    return deck


def pile2list(pile):
    cards_list = []
    for each in pile:
        cards_list.extend(each)
    return cards_list

def cards2list(cards):
    cards_list = []
    for each in cards:
        cards_list.append(each.get_str())
    return cards_list


def encode_cards(cards):
    plane = np.zeros((34,4), dtype=int)
    cards = cards2list(cards)
    for card in list(set(cards)):
        index = card_encoding_dict[card]
        num = cards.count(card)
        plane[index][:num] = 1
    return plane


'''
mahjong/__init__.py
'''
from mahjong.dealer import MahjongDealer as Dealer
from mahjong.card import MahjongCard as Card
from mahjong.player import MahjongPlayer as Player
from mahjong.judger import MahjongJudger as Judger
from mahjong.round import MahjongRound as Round
from mahjong.game import MahjongGame as Game



'''
mahjong/round.py
'''

class MahjongRound:

    def __init__(self, judger, dealer, num_players, np_random):
        ''' Initialize the round class

        Args:
            judger (object): the object of MahjongJudger
            dealer (object): the object of MahjongDealer
            num_players (int): the number of players in game
        '''
        self.np_random = np_random
        self.judger = judger
        self.dealer = dealer
        self.target = None
        self.current_player = 0
        self.last_player = None
        self.num_players = num_players
        self.direction = 1
        self.played_cards = []
        self.is_over = False
        self.player_before_act = 0
        self.prev_status = None
        self.valid_act = False
        self.last_cards = []

    def proceed_round(self, players, action):
        ''' Call other Classes's functions to keep one round running

        Args:
            player (object): object of UnoPlayer
            action (str): string of legal action
        '''
        #hand_len = [len(p.hand) for p in players]
        #pile_len = [sum([len([c for c in p]) for p in pp.pile]) for pp in players]
        #total_len = [i + j for i, j in zip(hand_len, pile_len)]
        if action == 'stand':
            (valid_act, player, cards) = self.judger.judge_chow(self.dealer, players, self.last_player)
            if valid_act:
                self.valid_act = valid_act
                self.last_cards = cards
                self.last_player = self.current_player
                self.current_player = player.player_id
            else:
                self.last_player = self.current_player
                self.current_player = (self.player_before_act + 1) % 4
                self.dealer.deal_cards(players[self.current_player], 1)
                self.valid_act = False

        elif action == 'gong':
            players[self.current_player].gong(self.dealer, self.last_cards)
            self.last_player = self.current_player
            self.valid_act = False

        elif action == 'pong':
            players[self.current_player].pong(self.dealer, self.last_cards)
            self.last_player = self.current_player
            self.valid_act = False

        elif action == 'chow':
            players[self.current_player].chow(self.dealer, self.last_cards)
            self.last_player = self.current_player
            self.valid_act = False

        else: # Play game: Proceed to next player
            players[self.current_player].play_card(self.dealer, action)
            self.player_before_act = self.current_player
            self.last_player = self.current_player
            (valid_act, player, cards) = self.judger.judge_pong_gong(self.dealer, players, self.last_player)
            if valid_act:
                self.valid_act = valid_act
                self.last_cards = cards
                self.last_player = self.current_player
                self.current_player = player.player_id
            else:
                self.last_player = self.current_player
                self.current_player = (self.current_player + 1) % 4
                self.dealer.deal_cards(players[self.current_player], 1)

        #hand_len = [len(p.hand) for p in players]
        #pile_len = [sum([len([c for c in p]) for p in pp.pile]) for pp in players]
        #total_len = [i + j for i, j in zip(hand_len, pile_len)]

    def get_state(self, players, player_id):
        ''' Get player's state

        Args:
            players (list): The list of MahjongPlayer
            player_id (int): The id of the player
        Return:
            state (dict): The information of the state
        '''
        state = {}
        #(valid_act, player, cards) = self.judger.judge_pong_gong(self.dealer, players, self.last_player)
        if self.valid_act: # PONG/GONG/CHOW
            state['valid_act'] = [self.valid_act, 'stand']
            state['table'] = self.dealer.table
            state['player'] = self.current_player
            state['current_hand'] = players[self.current_player].hand
            state['players_pile'] = {p.player_id: p.pile for p in players}
            state['action_cards'] = self.last_cards # For doing action (pong, chow, gong)
        else: # Regular Play
            state['valid_act'] = ['play']
            state['table'] = self.dealer.table
            state['player'] = self.current_player
            state['current_hand'] = players[player_id].hand
            state['players_pile'] = {p.player_id: p.pile for p in players}
            state['action_cards'] = players[player_id].hand # For doing action (pong, chow, gong)
        return state



