'''
blackjack/base.py
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
blackjack/player.py
'''
class BlackjackPlayer:

    def __init__(self, player_id, np_random):
        ''' Initialize a Blackjack player class

        Args:
            player_id (int): id for the player
        '''
        self.np_random = np_random
        self.player_id = player_id
        self.hand = []
        self.status = 'alive'
        self.score = 0

    def get_player_id(self):
        ''' Return player's id
        '''
        return self.player_id


'''
blackjack/dealer.py
'''
import numpy as np
from blackjack import Card

def init_standard_deck():
    ''' Initialize a standard deck of 52 cards

    Returns:
        (list): A list of Card object
    '''
    suit_list = ['S', 'H', 'D', 'C']
    rank_list = ['A', '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K']
    res = [Card(suit, rank) for suit in suit_list for rank in rank_list]
    return res

class BlackjackDealer:

    def __init__(self, np_random, num_decks=1):
        ''' Initialize a Blackjack dealer class
        '''
        self.np_random = np_random
        self.num_decks = num_decks
        self.deck = init_standard_deck()
        if self.num_decks not in [0, 1]:  # 0 indicates infinite decks of cards
            self.deck = self.deck * self.num_decks  # copy m standard decks of cards
        self.shuffle()
        self.hand = []
        self.status = 'alive'
        self.score = 0

    def shuffle(self):
        ''' Shuffle the deck
        '''
        shuffle_deck = np.array(self.deck)
        self.np_random.shuffle(shuffle_deck)
        self.deck = list(shuffle_deck)

    def deal_card(self, player):
        ''' Distribute one card to the player

        Args:
            player_id (int): the target player's id
        '''
        idx = self.np_random.choice(len(self.deck))
        card = self.deck[idx]
        if self.num_decks != 0:  # If infinite decks, do not pop card from deck
            self.deck.pop(idx)
        # card = self.deck.pop()
        player.hand.append(card)


'''
blackjack/judger.py
'''
class BlackjackJudger:
    def __init__(self, np_random):
        ''' Initialize a BlackJack judger class
        '''
        self.np_random = np_random
        self.rank2score = {"A":11, "2":2, "3":3, "4":4, "5":5, "6":6, "7":7, "8":8, "9":9, "T":10, "J":10, "Q":10, "K":10}

    def judge_round(self, player):
        ''' Judge the target player's status

        Args:
            player (int): target player's id

        Returns:
            status (str): the status of the target player
            score (int): the current score of the player
        '''
        score = self.judge_score(player.hand)
        if score <= 21:
            return "alive", score
        else:
            return "bust", score

    def judge_game(self, game, game_pointer):
        ''' Judge the winner of the game

        Args:
            game (class): target game class
        '''
        '''
                game.winner['dealer'] doesn't need anymore if we change code like this

                player bust (whether dealer bust or not) => game.winner[playerX] = -1
                player and dealer tie => game.winner[playerX] = 1
                dealer bust and player not bust => game.winner[playerX] = 2
                player get higher score than dealer => game.winner[playerX] = 2
                dealer get higher score than player => game.winner[playerX] = -1
                game.winner[playerX] = 0 => the game is still ongoing
                '''

        if game.players[game_pointer].status == 'bust':
            game.winner['player' + str(game_pointer)] = -1
        elif game.dealer.status == 'bust':
            game.winner['player' + str(game_pointer)] = 2
        else:
            if game.players[game_pointer].score > game.dealer.score:
                game.winner['player' + str(game_pointer)] = 2
            elif game.players[game_pointer].score < game.dealer.score:
                game.winner['player' + str(game_pointer)] = -1
            else:
                game.winner['player' + str(game_pointer)] = 1

    def judge_score(self, cards):
        ''' Judge the score of a given cards set

        Args:
            cards (list): a list of cards

        Returns:
            score (int): the score of the given cards set
        '''
        score = 0
        count_a = 0
        for card in cards:
            card_score = self.rank2score[card.rank]
            score += card_score
            if card.rank == 'A':
                count_a += 1
        while score > 21 and count_a > 0:
            count_a -= 1
            score -= 10
        return score


'''
blackjack/game.py
'''
from copy import deepcopy
import numpy as np

from blackjack import Dealer
from blackjack import Player
from blackjack import Judger

class BlackjackGame:

    def __init__(self, allow_step_back=False):
        ''' Initialize the class Blackjack Game
        '''
        self.allow_step_back = allow_step_back
        self.np_random = np.random.RandomState()

    def configure(self, game_config):
        ''' Specifiy some game specific parameters, such as number of players
        '''
        self.num_players = game_config['game_num_players']
        self.num_decks = game_config['game_num_decks']

    def init_game(self):
        ''' Initialilze the game

        Returns:
            state (dict): the first state of the game
            player_id (int): current player's id
        '''
        self.dealer = Dealer(self.np_random, self.num_decks)

        self.players = []
        for i in range(self.num_players):
            self.players.append(Player(i, self.np_random))

        self.judger = Judger(self.np_random)

        for i in range(2):
            for j in range(self.num_players):
                self.dealer.deal_card(self.players[j])
            self.dealer.deal_card(self.dealer)

        for i in range(self.num_players):
            self.players[i].status, self.players[i].score = self.judger.judge_round(self.players[i])

        self.dealer.status, self.dealer.score = self.judger.judge_round(self.dealer)

        self.winner = {'dealer': 0}
        for i in range(self.num_players):
            self.winner['player' + str(i)] = 0

        self.history = []
        self.game_pointer = 0

        return self.get_state(self.game_pointer), self.game_pointer

    def step(self, action):
        ''' Get the next state

        Args:
            action (str): a specific action of blackjack. (Hit or Stand)

        Returns:/
            dict: next player's state
            int: next plater's id
        '''
        if self.allow_step_back:
            p = deepcopy(self.players[self.game_pointer])
            d = deepcopy(self.dealer)
            w = deepcopy(self.winner)
            self.history.append((d, p, w))

        next_state = {}
        # Play hit
        if action != "stand":
            self.dealer.deal_card(self.players[self.game_pointer])
            self.players[self.game_pointer].status, self.players[self.game_pointer].score = self.judger.judge_round(
                self.players[self.game_pointer])
            if self.players[self.game_pointer].status == 'bust':
                # game over, set up the winner, print out dealer's hand # If bust, pass the game pointer
                if self.game_pointer >= self.num_players - 1:
                    while self.judger.judge_score(self.dealer.hand) < 17:
                        self.dealer.deal_card(self.dealer)
                    self.dealer.status, self.dealer.score = self.judger.judge_round(self.dealer)
                    for i in range(self.num_players):
                        self.judger.judge_game(self, i) 
                    self.game_pointer = 0
                else:
                    self.game_pointer += 1

                
        elif action == "stand": # If stand, first try to pass the pointer, if it's the last player, dealer deal for himself, then judge game for everyone using a loop
            self.players[self.game_pointer].status, self.players[self.game_pointer].score = self.judger.judge_round(
                self.players[self.game_pointer])
            if self.game_pointer >= self.num_players - 1:
                while self.judger.judge_score(self.dealer.hand) < 17:
                    self.dealer.deal_card(self.dealer)
                self.dealer.status, self.dealer.score = self.judger.judge_round(self.dealer)
                for i in range(self.num_players):
                    self.judger.judge_game(self, i) 
                self.game_pointer = 0
            else:
                self.game_pointer += 1


            
            

        hand = [card.get_index() for card in self.players[self.game_pointer].hand]

        if self.is_over():
            dealer_hand = [card.get_index() for card in self.dealer.hand]
        else:
            dealer_hand = [card.get_index() for card in self.dealer.hand[1:]]

        for i in range(self.num_players):
            next_state['player' + str(i) + ' hand'] = [card.get_index() for card in self.players[i].hand]
        next_state['dealer hand'] = dealer_hand
        next_state['actions'] = ('hit', 'stand')
        next_state['state'] = (hand, dealer_hand)

        

        return next_state, self.game_pointer

    def step_back(self):
        ''' Return to the previous state of the game

        Returns:
            Status (bool): check if the step back is success or not
        '''
        #while len(self.history) > 0:
        if len(self.history) > 0:
            self.dealer, self.players[self.game_pointer], self.winner = self.history.pop()
            return True
        return False

    def get_num_players(self):
        ''' Return the number of players in blackjack

        Returns:
            number_of_player (int): blackjack only have 1 player
        '''
        return self.num_players

    @staticmethod
    def get_num_actions():
        ''' Return the number of applicable actions

        Returns:
            number_of_actions (int): there are only two actions (hit and stand)
        '''
        return 2

    def get_player_id(self):
        ''' Return the current player's id

        Returns:
            player_id (int): current player's id
        '''
        return self.game_pointer

    def get_state(self, player_id):
        ''' Return player's state

        Args:
            player_id (int): player id

        Returns:
            state (dict): corresponding player's state
        '''
        '''
                before change state only have two keys (action, state)
                but now have more than 4 keys (action, state, player0 hand, player1 hand, ... , dealer hand)
                Although key 'state' have duplicated information with key 'player hand' and 'dealer hand', I couldn't remove it because of other codes
                To remove it, we need to change dqn agent too in my opinion
                '''
        state = {}
        state['actions'] = ('hit', 'stand')
        hand = [card.get_index() for card in self.players[player_id].hand]
        if self.is_over():
            dealer_hand = [card.get_index() for card in self.dealer.hand]
        else:
            dealer_hand = [card.get_index() for card in self.dealer.hand[1:]]

        for i in range(self.num_players):
            state['player' + str(i) + ' hand'] = [card.get_index() for card in self.players[i].hand]
        state['dealer hand'] = dealer_hand
        state['state'] = (hand, dealer_hand)

        return state

    def is_over(self):
        ''' Check if the game is over

        Returns:
            status (bool): True/False
        '''
        '''
                I should change here because judger and self.winner is changed too
                '''
        for i in range(self.num_players):
            if self.winner['player' + str(i)] == 0:
                return False

        return True


'''
blackjack/__init__.py
'''
from blackjack.base import Card as Card
from blackjack.dealer import BlackjackDealer as Dealer
from blackjack.judger import BlackjackJudger as Judger
from blackjack.player import BlackjackPlayer as Player
from blackjack.game import BlackjackGame as Game



