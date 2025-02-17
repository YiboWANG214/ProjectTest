'''
limitholdem/base.py
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
limitholdem/player.py
'''
from enum import Enum


class PlayerStatus(Enum):
    ALIVE = 0
    FOLDED = 1
    ALLIN = 2


class LimitHoldemPlayer:

    def __init__(self, player_id, np_random):
        """
        Initialize a player.

        Args:
            player_id (int): The id of the player
        """
        self.np_random = np_random
        self.player_id = player_id
        self.hand = []
        self.status = PlayerStatus.ALIVE

        # The chips that this player has put in until now
        self.in_chips = 0

    def get_state(self, public_cards, all_chips, legal_actions):
        """
        Encode the state for the player

        Args:
            public_cards (list): A list of public cards that seen by all the players
            all_chips (int): The chips that all players have put in

        Returns:
            (dict): The state of the player
        """
        return {
            'hand': [c.get_index() for c in self.hand],
            'public_cards': [c.get_index() for c in public_cards],
            'all_chips': all_chips,
            'my_chips': self.in_chips,
            'legal_actions': legal_actions
        }

    def get_player_id(self):
        return self.player_id


'''
limitholdem/dealer.py
'''
from limitholdem.utils import init_standard_deck

class LimitHoldemDealer:
    def __init__(self, np_random):
        self.np_random = np_random
        self.deck = init_standard_deck()
        self.shuffle()
        self.pot = 0

    def shuffle(self):
        self.np_random.shuffle(self.deck)

    def deal_card(self):
        """
        Deal one card from the deck

        Returns:
            (Card): The drawn card from the deck
        """
        return self.deck.pop()


'''
limitholdem/judger.py
'''
from limitholdem.utils import compare_hands
import numpy as np


class LimitHoldemJudger:
    """The Judger class for limit texas holdem"""

    def __init__(self, np_random):
        self.np_random = np_random

    def judge_game(self, players, hands):
        """
        Judge the winner of the game.

        Args:
            players (list): The list of players who play the game
            hands (list): The list of hands that from the players

        Returns:
            (list): Each entry of the list corresponds to one entry of the
        """
        # Convert the hands into card indexes
        hands = [[card.get_index() for card in hand] if hand is not None else None for hand in hands]
        
        in_chips = [p.in_chips for p in players]
        remaining = sum(in_chips)
        payoffs = [0] * len(hands)
        while remaining > 0:
            winners = compare_hands(hands)
            each_win = self.split_pots_among_players(in_chips, winners)
            
            for i in range(len(players)):
                if winners[i]:
                    remaining -= each_win[i]
                    payoffs[i] += each_win[i] - in_chips[i]
                    hands[i] = None
                    in_chips[i] = 0
                elif in_chips[i] > 0:
                    payoffs[i] += each_win[i] - in_chips[i]
                    in_chips[i] = each_win[i]
                    
        assert sum(payoffs) == 0
        return payoffs

    def split_pot_among_players(self, in_chips, winners):
        """
        Splits the next (side) pot among players.
        Function is called in loop by distribute_pots_among_players until all chips are allocated.

        Args:
            in_chips (list): List with number of chips bet not yet distributed for each player
            winners (list): List with 1 if the player is among winners else 0

        Returns:
            (list): Of how much chips each player get after this pot has been split and list of chips left to distribute
        """
        nb_winners_in_pot = sum((winners[i] and in_chips[i] > 0) for i in range(len(in_chips)))
        nb_players_in_pot = sum(in_chips[i] > 0 for i in range(len(in_chips)))
        if nb_winners_in_pot == 0 or nb_winners_in_pot == nb_players_in_pot:
            # no winner or all winners for this pot
            allocated = list(in_chips)  # we give back their chips to each players in this pot
            in_chips_after = len(in_chips) * [0]  # no more chips to distribute
        else:
            amount_in_pot_by_player = min(v for v in in_chips if v > 0)
            how_much_one_win, remaining = divmod(amount_in_pot_by_player * nb_players_in_pot, nb_winners_in_pot)
            '''
            In the event of a split pot that cannot be divided equally for every winner, the winner who is sitting 
            closest to the left of the dealer receives the remaining differential in chips cf 
            https://www.betclic.fr/poker/house-rules--play-safely--betclic-poker-cpok_rules to simplify and as this 
            case is very rare, we will give the remaining differential in chips to a random winner
            '''
            allocated = len(in_chips) * [0]
            in_chips_after = list(in_chips)
            for i in range(len(in_chips)):  # iterate on all players
                if in_chips[i] == 0:  # player not in pot
                    continue
                if winners[i]:
                    allocated[i] += how_much_one_win
                in_chips_after[i] -= amount_in_pot_by_player
            if remaining > 0:
                random_winning_player = self.np_random.choice(
                    [i for i in range(len(winners)) if winners[i] and in_chips[i] > 0])
                allocated[random_winning_player] += remaining
        assert sum(in_chips[i] - in_chips_after[i] for i in range(len(in_chips))) == sum(allocated)
        return allocated, in_chips_after

    def split_pots_among_players(self, in_chips_initial, winners):
        """
        Splits main pot and side pots among players (to handle special case of all-in players).

        Args:
            in_chips_initial (list): List with number of chips bet for each player
            winners (list): List with 1 if the player is among winners else 0

        Returns:
            (list): List of how much chips each player get back after all pots have been split
        """
        in_chips = list(in_chips_initial)
        assert len(in_chips) == len(winners)
        assert all(v == 0 or v == 1 for v in winners)
        assert sum(winners) >= 1  # there must be at least one winner
        allocated = np.zeros(len(in_chips), dtype=int)
        while any(v > 0 for v in in_chips):  # while there are still chips to allocate
            allocated_current_pot, in_chips = self.split_pot_among_players(in_chips, winners)
            allocated += allocated_current_pot  # element-wise addition
        assert all(chips >= 0 for chips in allocated)  # check that all players got a non negative amount of chips
        assert sum(in_chips_initial) == sum(allocated)  # check that all chips bet have been allocated
        return list(allocated)


'''
limitholdem/game.py
'''
from copy import deepcopy, copy
import numpy as np

from limitholdem import Dealer
from limitholdem import Player, PlayerStatus
from limitholdem import Judger
from limitholdem import Round


class LimitHoldemGame:
    def __init__(self, allow_step_back=False, num_players=2):
        """Initialize the class limit holdem game"""
        self.allow_step_back = allow_step_back
        self.np_random = np.random.RandomState()

        # Some configurations of the game
        # These arguments can be specified for creating new games

        # Small blind and big blind
        self.small_blind = 1
        self.big_blind = 2 * self.small_blind

        # Raise amount and allowed times
        self.raise_amount = self.big_blind
        self.allowed_raise_num = 4

        self.num_players = num_players

        # Save betting history
        self.history_raise_nums = [0 for _ in range(4)]

        self.dealer = None
        self.players = None
        self.judger = None
        self.public_cards = None
        self.game_pointer = None
        self.round = None
        self.round_counter = None
        self.history = None
        self.history_raises_nums = None

    def configure(self, game_config):
        """Specify some game specific parameters, such as number of players"""
        self.num_players = game_config['game_num_players']

    def init_game(self):
        """
        Initialize the game of limit texas holdem

        This version supports two-player limit texas holdem

        Returns:
            (tuple): Tuple containing:

                (dict): The first state of the game
                (int): Current player's id
        """
        # Initialize a dealer that can deal cards
        self.dealer = Dealer(self.np_random)

        # Initialize two players to play the game
        self.players = [Player(i, self.np_random) for i in range(self.num_players)]

        # Initialize a judger class which will decide who wins in the end
        self.judger = Judger(self.np_random)

        # Deal cards to each  player to prepare for the first round
        for i in range(2 * self.num_players):
            self.players[i % self.num_players].hand.append(self.dealer.deal_card())

        # Initialize public cards
        self.public_cards = []

        # Randomly choose a small blind and a big blind
        s = self.np_random.randint(0, self.num_players)
        b = (s + 1) % self.num_players
        self.players[b].in_chips = self.big_blind
        self.players[s].in_chips = self.small_blind

        # The player next to the big blind plays the first
        self.game_pointer = (b + 1) % self.num_players

        # Initialize a bidding round, in the first round, the big blind and the small blind needs to
        # be passed to the round for processing.
        self.round = Round(raise_amount=self.raise_amount,
                           allowed_raise_num=self.allowed_raise_num,
                           num_players=self.num_players,
                           np_random=self.np_random)

        self.round.start_new_round(game_pointer=self.game_pointer, raised=[p.in_chips for p in self.players])

        # Count the round. There are 4 rounds in each game.
        self.round_counter = 0

        # Save the history for stepping back to the last state.
        self.history = []

        state = self.get_state(self.game_pointer)

        # Save betting history
        self.history_raise_nums = [0 for _ in range(4)]

        return state, self.game_pointer

    def step(self, action):
        """
        Get the next state

        Args:
            action (str): a specific action. (call, raise, fold, or check)

        Returns:
            (tuple): Tuple containing:

                (dict): next player's state
                (int): next player id
        """
        if self.allow_step_back:
            # First snapshot the current state
            r = deepcopy(self.round)
            b = self.game_pointer
            r_c = self.round_counter
            d = deepcopy(self.dealer)
            p = deepcopy(self.public_cards)
            ps = deepcopy(self.players)
            rn = copy(self.history_raise_nums)
            self.history.append((r, b, r_c, d, p, ps, rn))

        # Then we proceed to the next round
        self.game_pointer = self.round.proceed_round(self.players, action)

        # Save the current raise num to history
        self.history_raise_nums[self.round_counter] = self.round.have_raised

        # If a round is over, we deal more public cards
        if self.round.is_over():
            # For the first round, we deal 3 cards
            if self.round_counter == 0:
                self.public_cards.append(self.dealer.deal_card())
                self.public_cards.append(self.dealer.deal_card())
                self.public_cards.append(self.dealer.deal_card())

            # For the following rounds, we deal only 1 card
            elif self.round_counter <= 2:
                self.public_cards.append(self.dealer.deal_card())

            # Double the raise amount for the last two rounds
            if self.round_counter == 1:
                self.round.raise_amount = 2 * self.raise_amount

            self.round_counter += 1
            self.round.start_new_round(self.game_pointer)

        state = self.get_state(self.game_pointer)

        return state, self.game_pointer

    def step_back(self):
        """
        Return to the previous state of the game

        Returns:
            (bool): True if the game steps back successfully
        """
        if len(self.history) > 0:
            self.round, self.game_pointer, self.round_counter, self.dealer, self.public_cards, \
                self.players, self.history_raises_nums = self.history.pop()
            return True
        return False

    def get_num_players(self):
        """
        Return the number of players in limit texas holdem

        Returns:
            (int): The number of players in the game
        """
        return self.num_players

    @staticmethod
    def get_num_actions():
        """
        Return the number of applicable actions

        Returns:
            (int): The number of actions. There are 4 actions (call, raise, check and fold)
        """
        return 4

    def get_player_id(self):
        """
        Return the current player's id

        Returns:
            (int): current player's id
        """
        return self.game_pointer

    def get_state(self, player):
        """
        Return player's state

        Args:
            player (int): player id

        Returns:
            (dict): The state of the player
        """
        chips = [self.players[i].in_chips for i in range(self.num_players)]
        legal_actions = self.get_legal_actions()
        state = self.players[player].get_state(self.public_cards, chips, legal_actions)
        state['raise_nums'] = self.history_raise_nums

        return state

    def is_over(self):
        """
        Check if the game is over

        Returns:
            (boolean): True if the game is over
        """
        alive_players = [1 if p.status in (PlayerStatus.ALIVE, PlayerStatus.ALLIN) else 0 for p in self.players]
        # If only one player is alive, the game is over.
        if sum(alive_players) == 1:
            return True

        # If all rounds are finished
        if self.round_counter >= 4:
            return True
        return False

    def get_payoffs(self):
        """
        Return the payoffs of the game

        Returns:
            (list): Each entry corresponds to the payoff of one player
        """
        hands = [p.hand + self.public_cards if p.status == PlayerStatus.ALIVE else None for p in self.players]
        chips_payoffs = self.judger.judge_game(self.players, hands)
        payoffs = np.array(chips_payoffs) / self.big_blind
        return payoffs

    def get_legal_actions(self):
        """
        Return the legal actions for current player

        Returns:
            (list): A list of legal actions
        """
        return self.round.get_legal_actions()


'''
limitholdem/utils.py
'''
import numpy as np
from limitholdem import Card


def init_standard_deck():
    ''' Initialize a standard deck of 52 cards

    Returns:
        (list): A list of Card object
    '''
    suit_list = ['S', 'H', 'D', 'C']
    rank_list = ['A', '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K']
    res = [Card(suit, rank) for suit in suit_list for rank in rank_list]
    return res


class Hand:
    def __init__(self, all_cards):
        self.all_cards = all_cards # two hand cards + five public cards
        self.category = 0
        #type of a players' best five cards, greater combination has higher number eg: 0:"Not_Yet_Evaluated" 1: "High_Card" , 9:"Straight_Flush"
        self.best_five = []
        #the largest combination of five cards in all the seven cards
        self.flush_cards = []
        #cards with same suit
        self.cards_by_rank = []
        #cards after sort
        self.product = 1
        #cards’ type indicator
        self.RANK_TO_STRING = {2: "2", 3: "3", 4: "4", 5: "5", 6: "6",
                               7: "7", 8: "8", 9: "9", 10: "T", 11: "J", 12: "Q", 13: "K", 14: "A"}
        self.STRING_TO_RANK = {v:k for k, v in self.RANK_TO_STRING.items()}
        self.RANK_LOOKUP = "23456789TJQKA"
        self.SUIT_LOOKUP = "SCDH"

    def get_hand_five_cards(self):
        '''
        Get the best five cards of a player
        Returns:
            (list): the best five cards among the seven cards of a player
        '''
        return self.best_five

    def _sort_cards(self):
        '''
        Sort all the seven cards ascendingly according to RANK_LOOKUP
        '''
        self.all_cards = sorted(
            self.all_cards, key=lambda card: self.RANK_LOOKUP.index(card[1]))

    def evaluateHand(self):
        """
        Evaluate all the seven cards, get the best combination catagory
        And pick the best five cards (for comparing in case 2 hands have the same Category) .
        """
        if len(self.all_cards) != 7:
            raise Exception(
                "There are not enough 7 cards in this hand, quit evaluation now ! ")

        self._sort_cards()
        self.cards_by_rank, self.product = self._getcards_by_rank(
            self.all_cards)

        if self._has_straight_flush():
            self.category = 9
            #Straight Flush
        elif self._has_four():
            self.category = 8
            #Four of a Kind
            self.best_five = self._get_Four_of_a_kind_cards()
        elif self._has_fullhouse():
            self.category = 7
            #Full house
            self.best_five = self._get_Fullhouse_cards()
        elif self._has_flush():
            self.category = 6
            #Flush
            i = len(self.flush_cards)
            self.best_five = [card for card in self.flush_cards[i-5:i]]
        elif self._has_straight(self.all_cards):
            self.category = 5
            #Straight
        elif self._has_three():
            self.category = 4
            #Three of a Kind
            self.best_five = self._get_Three_of_a_kind_cards()
        elif self._has_two_pairs():
            self.category = 3
            #Two Pairs
            self.best_five = self._get_Two_Pair_cards()
        elif self._has_pair():
            self.category = 2
            #One Pair
            self.best_five = self._get_One_Pair_cards()
        elif self._has_high_card():
            self.category = 1
            #High Card
            self.best_five = self._get_High_cards()

    def _has_straight_flush(self):
        '''
        Check the existence of straight_flush cards
        Returns:
            True: exist
            False: not exist
        '''
        self.flush_cards = self._getflush_cards()
        if len(self.flush_cards) > 0:
            straightflush_cards = self._get_straightflush_cards()
            if len(straightflush_cards) > 0:
                self.best_five = straightflush_cards
                return True
        return False

    def _get_straightflush_cards(self):
        '''
        Pick straight_flush cards
        Returns:
            (list): the straightflush cards
        '''
        straightflush_cards = self._get_straight_cards(self.flush_cards)
        return straightflush_cards

    def _getflush_cards(self):
        '''
        Pick flush cards
        Returns:
            (list): the flush cards
        '''
        card_string = ''.join(self.all_cards)
        for suit in self.SUIT_LOOKUP:
            suit_count = card_string.count(suit)
            if suit_count >= 5:
                flush_cards = [
                    card for card in self.all_cards if card[0] == suit]
                return flush_cards
        return []

    def _has_flush(self):
        '''
        Check the existence of flush cards
        Returns:
            True: exist
            False: not exist
        '''
        if len(self.flush_cards) > 0:
            return True
        else:
            return False

    def _has_straight(self, all_cards):
        '''
        Check the existence of straight cards
        Returns:
            True: exist
            False: not exist
        '''
        diff_rank_cards = self._get_different_rank_list(all_cards)
        self.best_five = self._get_straight_cards(diff_rank_cards)
        if len(self.best_five) != 0:
            return True
        else:
            return False
    @classmethod
    def _get_different_rank_list(self, all_cards):
        '''
        Get cards with different ranks, that is to say, remove duplicate-ranking cards, for picking straight cards' use
        Args:
            (list): two hand cards + five public cards
        Returns:
            (list): a list of cards with duplicate-ranking cards removed
        '''
        different_rank_list = []
        different_rank_list.append(all_cards[0])
        for card in all_cards:
            if(card[1] != different_rank_list[-1][1]):
                different_rank_list.append(card)
        return different_rank_list

    def _get_straight_cards(self, Cards):
        '''
        Pick straight cards
        Returns:
            (list): the straight cards
        '''
        ranks = [self.STRING_TO_RANK[c[1]] for c in Cards]

        highest_card = Cards[-1]
        if highest_card[1] == 'A':
            Cards.insert(0, highest_card)
            ranks.insert(0, 1)

        for i_last in range(len(ranks) - 1, 3, -1):
            if ranks[i_last-4] + 4 == ranks[i_last]:  # works because ranks are unique and sorted in ascending order
                return Cards[i_last-4:i_last+1]
        return []

    def _getcards_by_rank(self, all_cards):
        '''
        Get cards by rank
        Args:
            (list): # two hand cards + five public cards
        Return:
            card_group(list): cards after sort
            product(int):cards‘ type indicator
        '''
        card_group = []
        card_group_element = []
        product = 1
        prime_lookup = {0: 1, 1: 1, 2: 2, 3: 3, 4: 5}
        count = 0
        current_rank = 0

        for card in all_cards:
            rank = self.RANK_LOOKUP.index(card[1])
            if rank == current_rank:
                count += 1
                card_group_element.append(card)
            elif rank != current_rank:
                product *= prime_lookup[count]
                # Explanation :
                # if count == 2, then product *= 2
                # if count == 3, then product *= 3
                # if count == 4, then product *= 5
                # if there is a Quad, then product = 5 ( 4, 1, 1, 1) or product = 10 ( 4, 2, 1) or product= 15 (4,3)
                # if there is a Fullhouse, then product = 12 ( 3, 2, 2) or product = 9 (3, 3, 1) or product = 6 ( 3, 2, 1, 1)
                # if there is a Trip, then product = 3 ( 3, 1, 1, 1, 1)
                # if there is two Pair, then product = 4 ( 2, 1, 2, 1, 1) or product = 8 ( 2, 2, 2, 1)
                # if there is one Pair, then product = 2 (2, 1, 1, 1, 1, 1)
                # if there is HighCard, then product = 1 (1, 1, 1, 1, 1, 1, 1)
                card_group_element.insert(0, count)
                card_group.append(card_group_element)
                # reset counting
                count = 1
                card_group_element = []
                card_group_element.append(card)
                current_rank = rank
        # the For Loop misses operation for the last card
        # These 3 lines below to compensate that
        product *= prime_lookup[count]
        # insert the number of same rank card to the beginning of the
        card_group_element.insert(0, count)
        # after the loop, there is still one last card to add
        card_group.append(card_group_element)
        return card_group, product

    def _has_four(self):
        '''
        Check the existence of four cards
        Returns:
            True: exist
            False: not exist
        '''
        if self.product == 5 or self.product == 10 or self.product == 15:
            return True
        else:
            return False

    def _has_fullhouse(self):
        '''
        Check the existence of fullhouse cards
        Returns:
            True: exist
            False: not exist
        '''
        if self.product == 6 or self.product == 9 or self.product == 12:
            return True
        else:
            return False

    def _has_three(self):
        '''
        Check the existence of three cards
        Returns:
            True: exist
            False: not exist
        '''
        if self.product == 3:
            return True
        else:
            return False

    def _has_two_pairs(self):
        '''
        Check the existence of 2 pair cards
        Returns:
            True: exist
            False: not exist
        '''
        if self.product == 4 or self.product == 8:
            return True
        else:
            return False

    def _has_pair(self):
        '''
        Check the existence of 1 pair cards
        Returns:
            True: exist
            False: not exist
        '''
        if self.product == 2:
            return True
        else:
            return False

    def _has_high_card(self):
        '''
        Check the existence of high cards
        Returns:
            True: exist
            False: not exist
        '''
        if self.product == 1:
            return True
        else:
            return False

    def _get_Four_of_a_kind_cards(self):
        '''
        Get the four of a kind cards among a player's cards
        Returns:
            (list): best five hand cards after sort
        '''
        Four_of_a_Kind = []
        cards_by_rank = self.cards_by_rank
        cards_len = len(cards_by_rank)
        for i in reversed(range(cards_len)):
            if cards_by_rank[i][0] == 4:
                Four_of_a_Kind = cards_by_rank.pop(i)
                break
        # The Last cards_by_rank[The Second element]
        kicker = cards_by_rank[-1][1]
        Four_of_a_Kind[0] = kicker

        return Four_of_a_Kind

    def _get_Fullhouse_cards(self):
        '''
        Get the fullhouse cards among a player's cards
        Returns:
            (list): best five hand cards after sort
        '''
        Fullhouse = []
        cards_by_rank = self.cards_by_rank
        cards_len = len(cards_by_rank)
        for i in reversed(range(cards_len)):
            if cards_by_rank[i][0] == 3:
                Trips = cards_by_rank.pop(i)[1:4]
                break
        for i in reversed(range(cards_len - 1)):
            if cards_by_rank[i][0] >= 2:
                TwoPair = cards_by_rank.pop(i)[1:3]
                break
        Fullhouse = TwoPair + Trips
        return Fullhouse

    def _get_Three_of_a_kind_cards(self):
        '''
        Get the three of a kind cards among a player's cards
        Returns:
            (list): best five hand cards after sort
        '''
        Trip_cards = []
        cards_by_rank = self.cards_by_rank
        cards_len = len(cards_by_rank)
        for i in reversed(range(cards_len)):
            if cards_by_rank[i][0] == 3:
                Trip_cards += cards_by_rank.pop(i)[1:4]
                break

        Trip_cards += cards_by_rank.pop(-1)[1:2]
        Trip_cards += cards_by_rank.pop(-1)[1:2]
        Trip_cards.reverse()
        return Trip_cards

    def _get_Two_Pair_cards(self):
        '''
        Get the two pair cards among a player's cards
        Returns:
            (list): best five hand cards after sort
        '''
        Two_Pair_cards = []
        cards_by_rank = self.cards_by_rank
        cards_len = len(cards_by_rank)
        for i in reversed(range(cards_len)):
            if cards_by_rank[i][0] == 2 and len(Two_Pair_cards) < 3:
                Two_Pair_cards += cards_by_rank.pop(i)[1:3]

        Two_Pair_cards += cards_by_rank.pop(-1)[1:2]
        Two_Pair_cards.reverse()
        return Two_Pair_cards

    def _get_One_Pair_cards(self):
        '''
        Get the one pair cards among a player's cards
        Returns:
            (list): best five hand cards after sort
        '''
        One_Pair_cards = []
        cards_by_rank = self.cards_by_rank
        cards_len = len(cards_by_rank)
        for i in reversed(range(cards_len)):
            if cards_by_rank[i][0] == 2:
                One_Pair_cards += cards_by_rank.pop(i)[1:3]
                break

        One_Pair_cards += cards_by_rank.pop(-1)[1:2]
        One_Pair_cards += cards_by_rank.pop(-1)[1:2]
        One_Pair_cards += cards_by_rank.pop(-1)[1:2]
        One_Pair_cards.reverse()
        return One_Pair_cards

    def _get_High_cards(self):
        '''
        Get the high cards among a player's cards
        Returns:
            (list): best five hand cards after sort
        '''
        High_cards = self.all_cards[2:7]
        return High_cards

def compare_ranks(position, hands, winner):
    '''
    Compare cards in same position of plays' five handcards
    Args:
        position(int): the position of a card in a sorted handcard
        hands(list): cards of those players.
        e.g. hands = [['CT', 'ST', 'H9', 'B9', 'C2', 'C8', 'C7'], ['CJ', 'SJ', 'H9', 'B9', 'C2', 'C8', 'C7'], ['CT', 'ST', 'H9', 'B9', 'C2', 'C8', 'C7']]
        winner: array of same length than hands with 1 if the hand is among winners and 0 among losers
    Returns:
        new updated winner array
        [0, 1, 0]: player1 wins
        [1, 0, 0]: player0 wins
        [1, 1, 1]: draw
        [1, 1, 0]: player1 and player0 draws

    '''
    assert len(hands) == len(winner)
    RANKS = '23456789TJQKA'
    cards_figure_all_players = [None]*len(hands)  #cards without suit
    for i, hand in enumerate(hands):
        if winner[i]:
            cards = hands[i].get_hand_five_cards()
            if len(cards[0]) != 1:# remove suit
                for p in range(5):
                    cards[p] = cards[p][1:]
            cards_figure_all_players[i] = cards

    rival_ranks = [] # ranks of rival_figures
    for i, cards_figure in enumerate(cards_figure_all_players):
        if winner[i]:
            rank = cards_figure_all_players[i][position]
            rival_ranks.append(RANKS.index(rank))
        else:
            rival_ranks.append(-1)  # player has already lost
    new_winner = list(winner)
    for i, rival_rank in enumerate(rival_ranks):
        if rival_rank != max(rival_ranks):
            new_winner[i] = 0
    return new_winner

def determine_winner(key_index, hands, all_players, potential_winner_index):
    '''
    Find out who wins in the situation of having players with same highest hand_catagory
    Args:
        key_index(int): the position of a card in a sorted handcard
        hands(list): cards of those players with same highest hand_catagory.
        e.g. hands = [['CT', 'ST', 'H9', 'B9', 'C2', 'C8', 'C7'], ['CJ', 'SJ', 'H9', 'B9', 'C2', 'C8', 'C7'], ['CT', 'ST', 'H9', 'B9', 'C2', 'C8', 'C7']]
        all_players(list): all the players in this round, 0 for losing and 1 for winning or draw
        potential_winner_index(list): the positions of those players with same highest hand_catagory in all_players
    Returns:
        [0, 1, 0]: player1 wins
        [1, 0, 0]: player0 wins
        [1, 1, 1]: draw
        [1, 1, 0]: player1 and player0 draws

    '''
    winner = [1]*len(hands)
    i_index = 0
    while i_index < len(key_index) and sum(winner) > 1:
        index_break_tie = key_index[i_index]
        winner = compare_ranks(index_break_tie, hands, winner)
        i_index += 1
    for i in range(len(potential_winner_index)):
        if winner[i]:
            all_players[potential_winner_index[i]] = 1
    return all_players

def determine_winner_straight(hands, all_players, potential_winner_index):
    '''
    Find out who wins in the situation of having players all having a straight or straight flush
    Args:
        key_index(int): the position of a card in a sorted handcard
        hands(list): cards of those players which all have a straight or straight flush
        all_players(list): all the players in this round, 0 for losing and 1 for winning or draw
        potential_winner_index(list): the positions of those players with same highest hand_catagory in all_players
    Returns:
        [0, 1, 0]: player1 wins
        [1, 0, 0]: player0 wins
        [1, 1, 1]: draw
        [1, 1, 0]: player1 and player0 draws
    '''
    highest_ranks = []
    for hand in hands:
        highest_rank = hand.STRING_TO_RANK[hand.best_five[-1][1]]  # cards are sorted in ascending order
        highest_ranks.append(highest_rank)
    max_highest_rank = max(highest_ranks)
    for i_player in range(len(highest_ranks)):
        if highest_ranks[i_player] == max_highest_rank:
            all_players[potential_winner_index[i_player]] = 1
    return all_players

def determine_winner_four_of_a_kind(hands, all_players, potential_winner_index):
    '''
    Find out who wins in the situation of having players which all have a four of a kind
    Args:
        key_index(int): the position of a card in a sorted handcard
        hands(list): cards of those players with a four of a kind
        e.g. hands = [['CT', 'ST', 'H9', 'B9', 'C2', 'C8', 'C7'], ['CJ', 'SJ', 'H9', 'B9', 'C2', 'C8', 'C7'], ['CT', 'ST', 'H9', 'B9', 'C2', 'C8', 'C7']]
        all_players(list): all the players in this round, 0 for losing and 1 for winning or draw
        potential_winner_index(list): the positions of those players with same highest hand_catagory in all_players
    Returns:
        [0, 1, 0]: player1 wins
        [1, 0, 0]: player0 wins
        [1, 1, 1]: draw
        [1, 1, 0]: player1 and player0 draws
    '''
    ranks = []
    for hand in hands:
        rank_1 = hand.STRING_TO_RANK[hand.best_five[-1][1]]  # rank of the four of a kind
        rank_2 = hand.STRING_TO_RANK[hand.best_five[0][1]]  # rank of the kicker
        ranks.append((rank_1, rank_2))
    max_rank = max(ranks)
    for i, rank in enumerate(ranks):
        if rank == max_rank:
            all_players[potential_winner_index[i]] = 1
    return all_players

def compare_hands(hands):
    '''
    Compare all palyer's all seven cards
    Args:
        hands(list): cards of those players with same highest hand_catagory.
        e.g. hands = [['CT', 'ST', 'H9', 'B9', 'C2', 'C8', 'C7'], ['CJ', 'SJ', 'H9', 'B9', 'C2', 'C8', 'C7'], ['CT', 'ST', 'H9', 'B9', 'C2', 'C8', 'C7']]
    Returns:
        [0, 1, 0]: player1 wins
        [1, 0, 0]: player0 wins
        [1, 1, 1]: draw
        [1, 1, 0]: player1 and player0 draws

    if hands[0] == None:
        return [0, 1]
    elif hands[1] == None:
        return [1, 0]
    '''
    hand_category = [] #such as high_card, straight_flush, etc
    all_players = [0]*len(hands) #all the players in this round, 0 for losing and 1 for winning or draw
    if None in hands:
        fold_players = [i for i, j in enumerate(hands) if j is None]
        if len(fold_players) == len(all_players) - 1:
            for _ in enumerate(hands):
                if _[0] in fold_players:
                    all_players[_[0]] = 0
                else:
                    all_players[_[0]] = 1
            return all_players
        else:
            for _ in enumerate(hands):
                if hands[_[0]] is not None:
                    hand = Hand(hands[_[0]])
                    hand.evaluateHand()
                    hand_category.append(hand.category)
                elif hands[_[0]] is None:
                    hand_category.append(0)
    else:
            for i in enumerate(hands):
                hand = Hand(hands[i[0]])
                hand.evaluateHand()
                hand_category.append(hand.category)
    potential_winner_index = [i for i, j in enumerate(hand_category) if j == max(hand_category)]# potential winner are those with same max card_catagory

    return final_compare(hands, potential_winner_index, all_players)

def final_compare(hands, potential_winner_index, all_players):
    '''
    Find out the winners from those who didn't fold
    Args:
        hands(list): cards of those players with same highest hand_catagory.
        e.g. hands = [['CT', 'ST', 'H9', 'B9', 'C2', 'C8', 'C7'], ['CJ', 'SJ', 'H9', 'B9', 'C2', 'C8', 'C7'], ['CT', 'ST', 'H9', 'B9', 'C2', 'C8', 'C7']]
        potential_winner_index(list): index of those with same max card_catagory in all_players
        all_players(list): a list of all the player's win/lose situation, 0 for lose and 1 for win
    Returns:
        [0, 1, 0]: player1 wins
        [1, 0, 0]: player0 wins
        [1, 1, 1]: draw
        [1, 1, 0]: player1 and player0 draws

    if hands[0] == None:
        return [0, 1]
    elif hands[1] == None:
        return [1, 0]
    '''
    if len(potential_winner_index) == 1:
        all_players[potential_winner_index[0]] = 1
        return all_players
    elif len(potential_winner_index) > 1:
        # compare when having equal max categories
        equal_hands = []
        for _ in potential_winner_index:
            hand = Hand(hands[_])
            hand.evaluateHand()
            equal_hands.append(hand)
        hand = equal_hands[0]
        if hand.category == 8:
            return determine_winner_four_of_a_kind(equal_hands, all_players, potential_winner_index)
        if hand.category == 7:
            return determine_winner([2, 0], equal_hands, all_players, potential_winner_index)
        if hand.category == 4:
            return determine_winner([2, 1, 0], equal_hands, all_players, potential_winner_index)
        if hand.category == 3:
            return determine_winner([4, 2, 0], equal_hands, all_players, potential_winner_index)
        if hand.category == 2:
            return determine_winner([4, 2, 1, 0], equal_hands, all_players, potential_winner_index)
        if hand.category == 1 or hand.category == 6:
            return determine_winner([4, 3, 2, 1, 0], equal_hands, all_players, potential_winner_index)
        if hand.category in [5, 9]:
            return determine_winner_straight(equal_hands, all_players, potential_winner_index)


'''
limitholdem/__init__.py
'''
from limitholdem.base import Card as Card

from limitholdem.dealer import LimitHoldemDealer as Dealer
from limitholdem.judger import LimitHoldemJudger as Judger
from limitholdem.player import LimitHoldemPlayer as Player
from limitholdem.player import PlayerStatus
from limitholdem.round import LimitHoldemRound as Round
from limitholdem.game import LimitHoldemGame as Game



'''
limitholdem/round.py
'''
# -*- coding: utf-8 -*-
"""Limit texas holdem round class implementation"""


class LimitHoldemRound:
    """Round can call other Classes' functions to keep the game running"""

    def __init__(self, raise_amount, allowed_raise_num, num_players, np_random):
        """
        Initialize the round class

        Args:
            raise_amount (int): the raise amount for each raise
            allowed_raise_num (int): The number of allowed raise num
            num_players (int): The number of players
        """
        self.np_random = np_random
        self.game_pointer = None
        self.raise_amount = raise_amount
        self.allowed_raise_num = allowed_raise_num

        self.num_players = num_players

        # Count the number of raise
        self.have_raised = 0

        # Count the number without raise
        # If every player agree to not raise, the round is over
        self.not_raise_num = 0

        # Raised amount for each player
        self.raised = [0 for _ in range(self.num_players)]
        self.player_folded = None

    def start_new_round(self, game_pointer, raised=None):
        """
        Start a new bidding round

        Args:
            game_pointer (int): The game_pointer that indicates the next player
            raised (list): Initialize the chips for each player

        Note: For the first round of the game, we need to setup the big/small blind
        """
        self.game_pointer = game_pointer
        self.have_raised = 0
        self.not_raise_num = 0
        if raised:
            self.raised = raised
        else:
            self.raised = [0 for _ in range(self.num_players)]

    def proceed_round(self, players, action):
        """
        Call other classes functions to keep one round running

        Args:
            players (list): The list of players that play the game
            action (str): An legal action taken by the player

        Returns:
            (int): The game_pointer that indicates the next player
        """
        if action not in self.get_legal_actions():
            raise Exception('{} is not legal action. Legal actions: {}'.format(action, self.get_legal_actions()))

        if action == 'call':
            diff = max(self.raised) - self.raised[self.game_pointer]
            self.raised[self.game_pointer] = max(self.raised)
            players[self.game_pointer].in_chips += diff
            self.not_raise_num += 1

        elif action == 'raise':
            diff = max(self.raised) - self.raised[self.game_pointer] + self.raise_amount
            self.raised[self.game_pointer] = max(self.raised) + self.raise_amount
            players[self.game_pointer].in_chips += diff
            self.have_raised += 1
            self.not_raise_num = 1

        elif action == 'fold':
            players[self.game_pointer].status = 'folded'
            self.player_folded = True

        elif action == 'check':
            self.not_raise_num += 1

        self.game_pointer = (self.game_pointer + 1) % self.num_players

        # Skip the folded players
        while players[self.game_pointer].status == 'folded':
            self.game_pointer = (self.game_pointer + 1) % self.num_players

        return self.game_pointer

    def get_legal_actions(self):
        """
        Obtain the legal actions for the current player

        Returns:
           (list):  A list of legal actions
        """
        full_actions = ['call', 'raise', 'fold', 'check']

        # If the the number of raises already reaches the maximum number raises, we can not raise any more
        if self.have_raised >= self.allowed_raise_num:
            full_actions.remove('raise')

        # If the current chips are less than that of the highest one in the round, we can not check
        if self.raised[self.game_pointer] < max(self.raised):
            full_actions.remove('check')

        # If the current player has put in the chips that are more than others, we can not call
        if self.raised[self.game_pointer] == max(self.raised):
            full_actions.remove('call')

        return full_actions

    def is_over(self):
        """
        Check whether the round is over

        Returns:
            (boolean): True if the current round is over
        """
        if self.not_raise_num >= self.num_players:
            return True
        return False


