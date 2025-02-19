'''
gin_rummy/base.py
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
gin_rummy/player.py
'''
from typing import List

from gin_rummy import Card

import utils

import melding


class GinRummyPlayer:

    def __init__(self, player_id: int, np_random):
        ''' Initialize a GinRummy player class

        Args:
            player_id (int): id for the player
        '''
        self.np_random = np_random
        self.player_id = player_id
        self.hand = []  # type: List[Card]
        self.known_cards = []  # type: List[Card]  # opponent knows cards picked up by player and not yet discarded
        # memoization for speed
        self.meld_kinds_by_rank_id = [[] for _ in range(13)]  # type: List[List[List[Card]]]
        self.meld_run_by_suit_id = [[] for _ in range(4)]  # type: List[List[List[Card]]]

    def get_player_id(self) -> int:
        ''' Return player's id
        '''
        return self.player_id

    def get_meld_clusters(self) -> List[List[List[Card]]]:
        result = []  # type: List[List[List[Card]]]
        all_run_melds = [frozenset(meld_kind) for meld_kinds in self.meld_kinds_by_rank_id for meld_kind in meld_kinds]
        all_set_melds = [frozenset(meld_run) for meld_runs in self.meld_run_by_suit_id for meld_run in meld_runs]
        all_melds = all_run_melds + all_set_melds
        all_melds_count = len(all_melds)
        for i in range(0, all_melds_count):
            first_meld = all_melds[i]
            first_meld_list = list(first_meld)
            meld_cluster_1 = [first_meld_list]
            result.append(meld_cluster_1)
            for j in range(i + 1, all_melds_count):
                second_meld = all_melds[j]
                second_meld_list = list(second_meld)
                if not second_meld.isdisjoint(first_meld):
                    continue
                meld_cluster_2 = [first_meld_list, second_meld_list]
                result.append(meld_cluster_2)
                for k in range(j + 1, all_melds_count):
                    third_meld = all_melds[k]
                    third_meld_list = list(third_meld)
                    if not third_meld.isdisjoint(first_meld) or not third_meld.isdisjoint(second_meld):
                        continue
                    meld_cluster_3 = [first_meld_list, second_meld_list, third_meld_list]
                    result.append(meld_cluster_3)
        return result

    def did_populate_hand(self):
        self.meld_kinds_by_rank_id = [[] for _ in range(13)]
        self.meld_run_by_suit_id = [[] for _ in range(4)]
        all_set_melds = melding.get_all_set_melds(hand=self.hand)
        for set_meld in all_set_melds:
            rank_id = utils.get_rank_id(set_meld[0])
            self.meld_kinds_by_rank_id[rank_id].append(set_meld)
        all_run_melds = melding.get_all_run_melds(hand=self.hand)
        for run_meld in all_run_melds:
            suit_id = utils.get_suit_id(run_meld[0])
            self.meld_run_by_suit_id[suit_id].append(run_meld)

    def add_card_to_hand(self, card: Card):
        self.hand.append(card)
        self._increase_meld_kinds_by_rank_id(card=card)
        self._increase_run_kinds_by_suit_id(card=card)

    def remove_card_from_hand(self, card: Card):
        self.hand.remove(card)
        self._reduce_meld_kinds_by_rank_id(card=card)
        self._reduce_run_kinds_by_suit_id(card=card)

    def __str__(self):
        return "N" if self.player_id == 0 else "S"

    @staticmethod
    def short_name_of(player_id: int) -> str:
        return "N" if player_id == 0 else "S"

    @staticmethod
    def opponent_id_of(player_id: int) -> int:
        return (player_id + 1) % 2

    # private methods

    def _increase_meld_kinds_by_rank_id(self, card: Card):
        rank_id = utils.get_rank_id(card)
        meld_kinds = self.meld_kinds_by_rank_id[rank_id]
        if len(meld_kinds) == 0:
            card_rank = card.rank
            meld_kind = [card for card in self.hand if card.rank == card_rank]
            if len(meld_kind) >= 3:
                self.meld_kinds_by_rank_id[rank_id].append(meld_kind)
        else:  # must have all cards of given rank
            suits = ['S', 'H', 'D', 'C']
            max_kind_meld = [Card(suit, card.rank) for suit in suits]
            self.meld_kinds_by_rank_id[rank_id] = [max_kind_meld]
            for meld_card in max_kind_meld:
                self.meld_kinds_by_rank_id[rank_id].append([card for card in max_kind_meld if card != meld_card])

    def _reduce_meld_kinds_by_rank_id(self, card: Card):
        rank_id = utils.get_rank_id(card)
        meld_kinds = self.meld_kinds_by_rank_id[rank_id]
        if len(meld_kinds) > 1:
            suits = ['S', 'H', 'D', 'C']
            self.meld_kinds_by_rank_id[rank_id] = [[Card(suit, card.rank) for suit in suits if suit != card.suit]]
        else:
            self.meld_kinds_by_rank_id[rank_id] = []

    def _increase_run_kinds_by_suit_id(self, card: Card):
        suit_id = utils.get_suit_id(card=card)
        self.meld_run_by_suit_id[suit_id] = melding.get_all_run_melds_for_suit(cards=self.hand, suit=card.suit)

    def _reduce_run_kinds_by_suit_id(self, card: Card):
        suit_id = utils.get_suit_id(card=card)
        meld_runs = self.meld_run_by_suit_id[suit_id]
        self.meld_run_by_suit_id[suit_id] = [meld_run for meld_run in meld_runs if card not in meld_run]


'''
gin_rummy/judge.py
'''
from typing import TYPE_CHECKING

from typing import List, Tuple

from action_event import *
from scorers import GinRummyScorer
import melding
from gin_rummy_error import GinRummyProgramError

import utils


class GinRummyJudge:

    '''
        Judge decides legal actions for current player
    '''

    def __init__(self, game: 'GinRummyGame'):
        ''' Initialize the class GinRummyJudge
        :param game: GinRummyGame
        '''
        self.game = game
        self.scorer = GinRummyScorer()

    def get_legal_actions(self) -> List[ActionEvent]:
        """
        :return: List[ActionEvent] of legal actions
        """
        legal_actions = []  # type: List[ActionEvent]
        last_action = self.game.get_last_action()
        if last_action is None or \
                isinstance(last_action, DrawCardAction) or \
                isinstance(last_action, PickUpDiscardAction):
            current_player = self.game.get_current_player()
            going_out_deadwood_count = self.game.settings.going_out_deadwood_count
            hand = current_player.hand
            meld_clusters = current_player.get_meld_clusters()  # improve speed 2020-Apr
            knock_cards, gin_cards = _get_going_out_cards(meld_clusters=meld_clusters,
                                                          hand=hand,
                                                          going_out_deadwood_count=going_out_deadwood_count)
            if self.game.settings.is_allowed_gin and gin_cards:
                legal_actions = [GinAction()]
            else:
                cards_to_discard = [card for card in hand]
                if isinstance(last_action, PickUpDiscardAction):
                    if not self.game.settings.is_allowed_to_discard_picked_up_card:
                        picked_up_card = self.game.round.move_sheet[-1].card
                        cards_to_discard.remove(picked_up_card)
                discard_actions = [DiscardAction(card=card) for card in cards_to_discard]
                legal_actions = discard_actions
                if self.game.settings.is_allowed_knock:
                    if current_player.player_id == 0 or not self.game.settings.is_south_never_knocks:
                        if knock_cards:
                            knock_actions = [KnockAction(card=card) for card in knock_cards]
                            if not self.game.settings.is_always_knock:
                                legal_actions.extend(knock_actions)
                            else:
                                legal_actions = knock_actions
        elif isinstance(last_action, DeclareDeadHandAction):
            legal_actions = [ScoreNorthPlayerAction()]
        elif isinstance(last_action, GinAction):
            legal_actions = [ScoreNorthPlayerAction()]
        elif isinstance(last_action, DiscardAction):
            can_draw_card = len(self.game.round.dealer.stock_pile) > self.game.settings.stockpile_dead_card_count
            if self.game.settings.max_drawn_card_count < 52:  # NOTE: this
                drawn_card_actions = [action for action in self.game.actions if isinstance(action, DrawCardAction)]
                if len(drawn_card_actions) >= self.game.settings.max_drawn_card_count:
                    can_draw_card = False
            move_count = len(self.game.round.move_sheet)
            if move_count >= self.game.settings.max_move_count:
                legal_actions = [DeclareDeadHandAction()]  # prevent unlimited number of moves in a game
            elif can_draw_card:
                legal_actions = [DrawCardAction()]
                if self.game.settings.is_allowed_pick_up_discard:
                    legal_actions.append(PickUpDiscardAction())
            else:
                legal_actions = [DeclareDeadHandAction()]
                if self.game.settings.is_allowed_pick_up_discard:
                    legal_actions.append(PickUpDiscardAction())
        elif isinstance(last_action, KnockAction):
            legal_actions = [ScoreNorthPlayerAction()]
        elif isinstance(last_action, ScoreNorthPlayerAction):
            legal_actions = [ScoreSouthPlayerAction()]
        elif isinstance(last_action, ScoreSouthPlayerAction):
            pass
        else:
            raise Exception('get_legal_actions: unknown last_action={}'.format(last_action))
        return legal_actions


def get_going_out_cards(hand: List[Card], going_out_deadwood_count: int) -> Tuple[List[Card], List[Card]]:
    '''
    :param hand: List[Card] -- must have 11 cards
    :param going_out_deadwood_count: int
    :return List[Card], List[Card: cards in hand that be knocked, cards in hand that can be ginned
    '''
    if not len(hand) == 11:
        raise GinRummyProgramError("len(hand) is {}: should be 11.".format(len(hand)))
    meld_clusters = melding.get_meld_clusters(hand=hand)
    knock_cards, gin_cards = _get_going_out_cards(meld_clusters=meld_clusters,
                                                  hand=hand,
                                                  going_out_deadwood_count=going_out_deadwood_count)
    return list(knock_cards), list(gin_cards)


#
# private methods
#

def _get_going_out_cards(meld_clusters: List[List[List[Card]]],
                         hand: List[Card],
                         going_out_deadwood_count: int) -> Tuple[List[Card], List[Card]]:
    '''
    :param meld_clusters
    :param hand: List[Card] -- must have 11 cards
    :param going_out_deadwood_count: int
    :return List[Card], List[Card: cards in hand that be knocked, cards in hand that can be ginned
    '''
    if not len(hand) == 11:
        raise GinRummyProgramError("len(hand) is {}: should be 11.".format(len(hand)))
    knock_cards = set()
    gin_cards = set()
    for meld_cluster in meld_clusters:
        meld_cards = [card for meld_pile in meld_cluster for card in meld_pile]
        hand_deadwood = [card for card in hand if card not in meld_cards]  # hand has 11 cards
        if len(hand_deadwood) == 0:
            # all 11 cards are melded;
            # take gin_card as first card of first 4+ meld;
            # could also take gin_card as last card of 4+ meld, but won't do this.
            for meld_pile in meld_cluster:
                if len(meld_pile) >= 4:
                    gin_cards.add(meld_pile[0])
                    break
        elif len(hand_deadwood) == 1:
            card = hand_deadwood[0]
            gin_cards.add(card)
        else:
            hand_deadwood_values = [utils.get_deadwood_value(card) for card in hand_deadwood]
            hand_deadwood_count = sum(hand_deadwood_values)
            max_hand_deadwood_value = max(hand_deadwood_values, default=0)
            if hand_deadwood_count <= 10 + max_hand_deadwood_value:
                for card in hand_deadwood:
                    next_deadwood_count = hand_deadwood_count - utils.get_deadwood_value(card)
                    if next_deadwood_count <= going_out_deadwood_count:
                        knock_cards.add(card)
    return list(knock_cards), list(gin_cards)


'''
gin_rummy/move.py
'''
from typing import List

from gin_rummy import Player

from action_event import *

from gin_rummy_error import GinRummyProgramError


#
#   These classes are used to keep a move_sheet history of the moves in a round.
#

class GinRummyMove(object):
    pass


class PlayerMove(GinRummyMove):

    def __init__(self, player: Player, action: ActionEvent):
        super().__init__()
        self.player = player
        self.action = action


class DealHandMove(GinRummyMove):

    def __init__(self, player_dealing: Player, shuffled_deck: List[Card]):
        super().__init__()
        self.player_dealing = player_dealing
        self.shuffled_deck = shuffled_deck

    def __str__(self):
        shuffled_deck_text = " ".join([str(card) for card in self.shuffled_deck])
        return "{} deal shuffled_deck=[{}]".format(self.player_dealing, shuffled_deck_text)


class DrawCardMove(PlayerMove):

    def __init__(self, player: Player, action: DrawCardAction, card: Card):
        super().__init__(player, action)
        if not isinstance(action, DrawCardAction):
            raise GinRummyProgramError("action must be DrawCardAction.")
        self.card = card

    def __str__(self):
        return "{} {} {}".format(self.player, self.action, str(self.card))


class PickupDiscardMove(PlayerMove):

    def __init__(self, player: Player, action: PickUpDiscardAction, card: Card):
        super().__init__(player, action)
        if not isinstance(action, PickUpDiscardAction):
            raise GinRummyProgramError("action must be PickUpDiscardAction.")
        self.card = card

    def __str__(self):
        return "{} {} {}".format(self.player, self.action, str(self.card))


class DeclareDeadHandMove(PlayerMove):

    def __init__(self, player: Player, action: DeclareDeadHandAction):
        super().__init__(player, action)
        if not isinstance(action, DeclareDeadHandAction):
            raise GinRummyProgramError("action must be DeclareDeadHandAction.")

    def __str__(self):
        return "{} {}".format(self.player, self.action)


class DiscardMove(PlayerMove):

    def __init__(self, player: Player, action: DiscardAction):
        super().__init__(player, action)
        if not isinstance(action, DiscardAction):
            raise GinRummyProgramError("action must be DiscardAction.")

    def __str__(self):
        return "{} {}".format(self.player, self.action)


class KnockMove(PlayerMove):

    def __init__(self, player: Player, action: KnockAction):
        super().__init__(player, action)
        if not isinstance(action, KnockAction):
            raise GinRummyProgramError("action must be KnockAction.")

    def __str__(self):
        return "{} {}".format(self.player, self.action)


class GinMove(PlayerMove):

    def __init__(self, player: Player, action: GinAction):
        super().__init__(player, action)
        if not isinstance(action, GinAction):
            raise GinRummyProgramError("action must be GinAction.")

    def __str__(self):
        return "{} {}".format(self.player, self.action)


class ScoreNorthMove(PlayerMove):

    def __init__(self, player: Player,
                 action: ScoreNorthPlayerAction,
                 best_meld_cluster: List[List[Card]],
                 deadwood_count: int):
        super().__init__(player, action)
        if not isinstance(action, ScoreNorthPlayerAction):
            raise GinRummyProgramError("action must be ScoreNorthPlayerAction.")
        self.best_meld_cluster = best_meld_cluster  # for information use only
        self.deadwood_count = deadwood_count  # for information use only

    def __str__(self):
        best_meld_cluster_str = [[str(card) for card in meld_pile] for meld_pile in self.best_meld_cluster]
        best_meld_cluster_text = "{}".format(best_meld_cluster_str)
        return "{} {} {} {}".format(self.player, self.action, self.deadwood_count, best_meld_cluster_text)


class ScoreSouthMove(PlayerMove):

    def __init__(self, player: Player,
                 action: ScoreSouthPlayerAction,
                 best_meld_cluster: List[List[Card]],
                 deadwood_count: int):
        super().__init__(player, action)
        if not isinstance(action, ScoreSouthPlayerAction):
            raise GinRummyProgramError("action must be ScoreSouthPlayerAction.")
        self.best_meld_cluster = best_meld_cluster  # for information use only
        self.deadwood_count = deadwood_count  # for information use only

    def __str__(self):
        best_meld_cluster_str = [[str(card) for card in meld_pile] for meld_pile in self.best_meld_cluster]
        best_meld_cluster_text = "{}".format(best_meld_cluster_str)
        return "{} {} {} {}".format(self.player, self.action, self.deadwood_count, best_meld_cluster_text)


'''
gin_rummy/dealer.py
'''
from gin_rummy import Player
import utils as utils


class GinRummyDealer:
    ''' Initialize a GinRummy dealer class
    '''
    def __init__(self, np_random):
        ''' Empty discard_pile, set shuffled_deck, set stock_pile
        '''
        self.np_random = np_random
        self.discard_pile = []  # type: List[Card]
        self.shuffled_deck = utils.get_deck()  # keep a copy of the shuffled cards at start of new hand
        self.np_random.shuffle(self.shuffled_deck)
        self.stock_pile = self.shuffled_deck.copy()  # type: List[Card]

    def deal_cards(self, player: Player, num: int):
        ''' Deal some cards from stock_pile to one player

        Args:
            player (Player): The Player object
            num (int): The number of cards to be dealt
        '''
        for _ in range(num):
            player.hand.append(self.stock_pile.pop())
        player.did_populate_hand()


'''
gin_rummy/settings.py
'''
from typing import Dict, Any

from enum import Enum


class DealerForRound(int, Enum):
    North = 0
    South = 1
    Random = 2


class Setting(str, Enum):
    dealer_for_round = "dealer_for_round"
    stockpile_dead_card_count = "stockpile_dead_card_count"
    going_out_deadwood_count = "going_out_deadwood_count"
    max_drawn_card_count = "max_drawn_card_count"
    max_move_count = "max_move_count"
    is_allowed_knock = "is_allowed_knock"
    is_allowed_gin = "is_allowed_gin"
    is_allowed_pick_up_discard = "is_allowed_pick_up_discard"
    is_allowed_to_discard_picked_up_card = "is_allowed_to_discard_picked_up_card"
    is_always_knock = "is_always_knock"
    is_south_never_knocks = "is_south_never_knocks"

    @staticmethod
    def default_setting() -> Dict['Setting', Any]:
        return {Setting.dealer_for_round: DealerForRound.Random,
                Setting.stockpile_dead_card_count: 2,
                Setting.going_out_deadwood_count: 10,  # Can specify going_out_deadwood_count before running game.
                Setting.max_drawn_card_count: 52,
                Setting.max_move_count: 200,  # prevent unlimited number of moves in a game
                Setting.is_allowed_knock: True,
                Setting.is_allowed_gin: True,
                Setting.is_allowed_pick_up_discard: True,
                Setting.is_allowed_to_discard_picked_up_card: False,
                Setting.is_always_knock: False,
                Setting.is_south_never_knocks: False
                }

    @staticmethod
    def simple_gin_rummy_setting():  # speeding up training 200213
        # North should be agent being trained.
        # North always deals.
        # South never knocks.
        # North always knocks when can.
        return {Setting.dealer_for_round: DealerForRound.North,
                Setting.stockpile_dead_card_count: 2,
                Setting.going_out_deadwood_count: 10,  # Can specify going_out_deadwood_count before running game.
                Setting.max_drawn_card_count: 52,
                Setting.max_move_count: 200,  # prevent unlimited number of moves in a game
                Setting.is_allowed_knock: True,
                Setting.is_allowed_gin: True,
                Setting.is_allowed_pick_up_discard: True,
                Setting.is_allowed_to_discard_picked_up_card: False,
                Setting.is_always_knock: True,
                Setting.is_south_never_knocks: True
                }


dealer_for_round = Setting.dealer_for_round
stockpile_dead_card_count = Setting.stockpile_dead_card_count
going_out_deadwood_count = Setting.going_out_deadwood_count
max_drawn_card_count = Setting.max_drawn_card_count
max_move_count = Setting.max_move_count
is_allowed_knock = Setting.is_allowed_knock
is_allowed_gin = Setting.is_allowed_gin
is_allowed_pick_up_discard = Setting.is_allowed_pick_up_discard
is_allowed_to_discard_picked_up_card = Setting.is_allowed_to_discard_picked_up_card
is_always_knock = Setting.is_always_knock
is_south_never_knocks = Setting.is_south_never_knocks


class Settings(object):

    def __init__(self):
        self.scorer_name = "GinRummyScorer"
        default_setting = Setting.default_setting()
        self.dealer_for_round = default_setting[Setting.dealer_for_round]
        self.stockpile_dead_card_count = default_setting[Setting.stockpile_dead_card_count]
        self.going_out_deadwood_count = default_setting[Setting.going_out_deadwood_count]
        self.max_drawn_card_count = default_setting[Setting.max_drawn_card_count]
        self.max_move_count = default_setting[Setting.max_move_count]
        self.is_allowed_knock = default_setting[Setting.is_allowed_knock]
        self.is_allowed_gin = default_setting[Setting.is_allowed_gin]
        self.is_allowed_pick_up_discard = default_setting[Setting.is_allowed_pick_up_discard]
        self.is_allowed_to_discard_picked_up_card = default_setting[Setting.is_allowed_to_discard_picked_up_card]
        self.is_always_knock = default_setting[Setting.is_always_knock]
        self.is_south_never_knocks = default_setting[Setting.is_south_never_knocks]

    def change_settings(self, config: Dict[Setting, Any]):
        corrected_config = Settings.get_config_with_invalid_settings_set_to_default_value(config=config)
        for key, value in corrected_config.items():
            if key == Setting.dealer_for_round:
                self.dealer_for_round = value
            elif key == Setting.stockpile_dead_card_count:
                self.stockpile_dead_card_count = value
            elif key == Setting.going_out_deadwood_count:
                self.going_out_deadwood_count = value
            elif key == Setting.max_drawn_card_count:
                self.max_drawn_card_count = value
            elif key == Setting.max_move_count:
                self.max_move_count = value
            elif key == Setting.is_allowed_knock:
                self.is_allowed_knock = value
            elif key == Setting.is_allowed_gin:
                self.is_allowed_gin = value
            elif key == Setting.is_allowed_pick_up_discard:
                self.is_allowed_pick_up_discard = value
            elif key == Setting.is_allowed_to_discard_picked_up_card:
                self.is_allowed_to_discard_picked_up_card = value
            elif key == Setting.is_always_knock:
                self.is_always_knock = value
            elif key == Setting.is_south_never_knocks:
                self.is_south_never_knocks = value

    def print_settings(self):
        print("========== Settings ==========")
        print("scorer_name={}".format(self.scorer_name))
        print("dealer_for_round={}".format(self.dealer_for_round))
        print("stockpile_dead_card_count={}".format(self.stockpile_dead_card_count))
        print("going_out_deadwood_count={}".format(self.going_out_deadwood_count))
        print("max_drawn_card_count={}".format(self.max_drawn_card_count))
        print("max_move_count={}".format(self.max_move_count))

        print("is_allowed_knock={}".format(self.is_allowed_knock))
        print("is_allowed_gin={}".format(self.is_allowed_gin))
        print("is_allowed_pick_up_discard={}".format(self.is_allowed_pick_up_discard))

        print("is_allowed_to_discard_picked_up_card={}".format(self.is_allowed_to_discard_picked_up_card))

        print("is_always_knock={}".format(self.is_always_knock))
        print("is_south_never_knocks={}".format(self.is_south_never_knocks))
        print("==============================")

    @staticmethod
    def get_config_with_invalid_settings_set_to_default_value(config: Dict[Setting, Any]) -> Dict[Setting, Any]:
        # Set each invalid setting to its default_value.
        result = config.copy()
        default_setting = Setting.default_setting()
        for key, value in config.items():
            if key == dealer_for_round and not isinstance(value, DealerForRound):
                result[dealer_for_round] = default_setting[dealer_for_round]
            elif key == stockpile_dead_card_count and not isinstance(value, int):
                result[stockpile_dead_card_count] = default_setting[stockpile_dead_card_count]
            elif key == going_out_deadwood_count and not isinstance(value, int):
                result[going_out_deadwood_count] = default_setting[going_out_deadwood_count]
            elif key == max_drawn_card_count and not isinstance(value, int):
                result[max_drawn_card_count] = default_setting[max_drawn_card_count]
            elif key == max_move_count and not isinstance(value, int):
                result[max_move_count] = default_setting[max_move_count]
            elif key == is_allowed_knock and not isinstance(value, bool):
                result[is_allowed_knock] = default_setting[is_allowed_knock]
            elif key == is_allowed_gin and not isinstance(value, bool):
                result[is_allowed_gin] = default_setting[is_allowed_gin]
            elif key == is_allowed_pick_up_discard and not isinstance(value, bool):
                result[is_allowed_pick_up_discard] = default_setting[is_allowed_pick_up_discard]
            elif key == is_allowed_to_discard_picked_up_card and not isinstance(value, bool):
                result[is_allowed_to_discard_picked_up_card] = default_setting[is_allowed_to_discard_picked_up_card]
            elif key == is_always_knock and not isinstance(value, bool):
                result[is_always_knock] = default_setting[is_always_knock]
            elif key == is_south_never_knocks and not isinstance(value, bool):
                result[is_south_never_knocks] = default_setting[is_south_never_knocks]
        return result



'''
gin_rummy/action_event.py
'''
from gin_rummy import Card

import utils as utils

# ====================================
# Action_ids:
#        0 -> score_player_0_id
#        1 -> score_player_1_id
#        2 -> draw_card_id
#        3 -> pick_up_discard_id
#        4 -> declare_dead_hand_id
#        5 -> gin_id
#        6 to 57 -> discard_id card_id
#        58 to 109 -> knock_id card_id
# ====================================

score_player_0_action_id = 0
score_player_1_action_id = 1
draw_card_action_id = 2
pick_up_discard_action_id = 3
declare_dead_hand_action_id = 4
gin_action_id = 5
discard_action_id = 6
knock_action_id = discard_action_id + 52


class ActionEvent(object):

    def __init__(self, action_id: int):
        self.action_id = action_id

    def __eq__(self, other):
        result = False
        if isinstance(other, ActionEvent):
            result = self.action_id == other.action_id
        return result

    @staticmethod
    def get_num_actions():
        ''' Return the number of possible actions in the game
        '''
        return knock_action_id + 52  # FIXME: sensitive to code changes 200213

    @staticmethod
    def decode_action(action_id) -> 'ActionEvent':
        ''' Action id -> the action_event in the game.

        Args:
            action_id (int): the id of the action

        Returns:
            action (ActionEvent): the action that will be passed to the game engine.
        '''
        if action_id == score_player_0_action_id:
            action_event = ScoreNorthPlayerAction()
        elif action_id == score_player_1_action_id:
            action_event = ScoreSouthPlayerAction()
        elif action_id == draw_card_action_id:
            action_event = DrawCardAction()
        elif action_id == pick_up_discard_action_id:
            action_event = PickUpDiscardAction()
        elif action_id == declare_dead_hand_action_id:
            action_event = DeclareDeadHandAction()
        elif action_id == gin_action_id:
            action_event = GinAction()
        elif action_id in range(discard_action_id, discard_action_id + 52):
            card_id = action_id - discard_action_id
            card = utils.get_card(card_id=card_id)
            action_event = DiscardAction(card=card)
        elif action_id in range(knock_action_id, knock_action_id + 52):
            card_id = action_id - knock_action_id
            card = utils.get_card(card_id=card_id)
            action_event = KnockAction(card=card)
        else:
            raise Exception("decode_action: unknown action_id={}".format(action_id))
        return action_event


class ScoreNorthPlayerAction(ActionEvent):

    def __init__(self):
        super().__init__(action_id=score_player_0_action_id)

    def __str__(self):
        return "score N"


class ScoreSouthPlayerAction(ActionEvent):

    def __init__(self):
        super().__init__(action_id=score_player_1_action_id)

    def __str__(self):
        return "score S"


class DrawCardAction(ActionEvent):

    def __init__(self):
        super().__init__(action_id=draw_card_action_id)

    def __str__(self):
        return "draw_card"


class PickUpDiscardAction(ActionEvent):

    def __init__(self):
        super().__init__(action_id=pick_up_discard_action_id)

    def __str__(self):
        return "pick_up_discard"


class DeclareDeadHandAction(ActionEvent):

    def __init__(self):
        super().__init__(action_id=declare_dead_hand_action_id)

    def __str__(self):
        return "declare_dead_hand"


class GinAction(ActionEvent):

    def __init__(self):
        super().__init__(action_id=gin_action_id)

    def __str__(self):
        return "gin"


class DiscardAction(ActionEvent):

    def __init__(self, card: Card):
        card_id = utils.get_card_id(card)
        super().__init__(action_id=discard_action_id + card_id)
        self.card = card

    def __str__(self):
        return "discard {}".format(str(self.card))


class KnockAction(ActionEvent):

    def __init__(self, card: Card):
        card_id = utils.get_card_id(card)
        super().__init__(action_id=knock_action_id + card_id)
        self.card = card

    def __str__(self):
        return "knock {}".format(str(self.card))


'''
gin_rummy/scorers.py
'''
from typing import TYPE_CHECKING

from typing import Callable

from action_event import *
from gin_rummy import Player
from move import ScoreNorthMove, ScoreSouthMove
from gin_rummy_error import GinRummyProgramError

import melding
import utils


class GinRummyScorer:

    def __init__(self, name: str = None, get_payoff: Callable[[Player, 'GinRummyGame'], int or float] = None):
        self.name = name if name is not None else "GinRummyScorer"
        self.get_payoff = get_payoff if get_payoff else get_payoff_gin_rummy_v1

    def get_payoffs(self, game: 'GinRummyGame'):
        payoffs = [0, 0]
        for i in range(2):
            player = game.round.players[i]
            payoff = self.get_payoff(player=player, game=game)
            payoffs[i] = payoff
        return payoffs


def get_payoff_gin_rummy_v0(player: Player, game: 'GinRummyGame') -> int:
    ''' Get the payoff of player: deadwood_count of player

    Returns:
        payoff (int or float): payoff for player (lower is better)
    '''
    moves = game.round.move_sheet
    if player.player_id == 0:
        score_player_move = moves[-2]
        if not isinstance(score_player_move, ScoreNorthMove):
            raise GinRummyProgramError("score_player_move must be ScoreNorthMove.")
    else:
        score_player_move = moves[-1]
        if not isinstance(score_player_move, ScoreSouthMove):
            raise GinRummyProgramError("score_player_move must be ScoreSouthMove.")
    deadwood_count = score_player_move.deadwood_count
    return deadwood_count


def get_payoff_gin_rummy_v1(player: Player, game: 'GinRummyGame') -> float:
    ''' Get the payoff of player:
            a) 1.0 if player gins
            b) 0.2 if player knocks
            c) -deadwood_count / 100 otherwise

    Returns:
        payoff (int or float): payoff for player (higher is better)
    '''
    # payoff is 1.0 if player gins
    # payoff is 0.2 if player knocks
    # payoff is -deadwood_count / 100 if otherwise
    # The goal is to have the agent learn how to knock and gin.
    # The negative payoff when the agent fails to knock or gin should encourage the agent to form melds.
    # The payoff is scaled to lie between -1 and 1.
    going_out_action = game.round.going_out_action
    going_out_player_id = game.round.going_out_player_id
    if going_out_player_id == player.player_id and isinstance(going_out_action, KnockAction):
        payoff = 0.2
    elif going_out_player_id == player.player_id and isinstance(going_out_action, GinAction):
        payoff = 1
    else:
        hand = player.hand
        best_meld_clusters = melding.get_best_meld_clusters(hand=hand)
        best_meld_cluster = [] if not best_meld_clusters else best_meld_clusters[0]
        deadwood_count = utils.get_deadwood_count(hand, best_meld_cluster)
        payoff = -deadwood_count / 100
    return payoff


'''
gin_rummy/game.py
'''
import numpy as np

from gin_rummy import Player
from round import GinRummyRound
from gin_rummy import Judger
from settings import Settings, DealerForRound

from action_event import *


class GinRummyGame:
    ''' Game class. This class will interact with outer environment.
    '''

    def __init__(self, allow_step_back=False):
        '''Initialize the class GinRummyGame
        '''
        self.allow_step_back = allow_step_back
        self.np_random = np.random.RandomState()
        self.judge = Judger(game=self)
        self.settings = Settings()
        self.actions = None  # type: List[ActionEvent] or None # must reset in init_game
        self.round = None  # round: GinRummyRound or None, must reset in init_game
        self.num_players = 2

    def init_game(self):
        ''' Initialize all characters in the game and start round 1
        '''
        dealer_id = self.np_random.choice([0, 1])
        if self.settings.dealer_for_round == DealerForRound.North:
            dealer_id = 0
        elif self.settings.dealer_for_round == DealerForRound.South:
            dealer_id = 1
        self.actions = []
        self.round = GinRummyRound(dealer_id=dealer_id, np_random=self.np_random)
        for i in range(2):
            num = 11 if i == 0 else 10
            player = self.round.players[(dealer_id + 1 + i) % 2]
            self.round.dealer.deal_cards(player=player, num=num)
        current_player_id = self.round.current_player_id
        state = self.get_state(player_id=current_player_id)
        return state, current_player_id

    def step(self, action: ActionEvent):
        ''' Perform game action and return next player number, and the state for next player
        '''
        if isinstance(action, ScoreNorthPlayerAction):
            self.round.score_player_0(action)
        elif isinstance(action, ScoreSouthPlayerAction):
            self.round.score_player_1(action)
        elif isinstance(action, DrawCardAction):
            self.round.draw_card(action)
        elif isinstance(action, PickUpDiscardAction):
            self.round.pick_up_discard(action)
        elif isinstance(action, DeclareDeadHandAction):
            self.round.declare_dead_hand(action)
        elif isinstance(action, GinAction):
            self.round.gin(action, going_out_deadwood_count=self.settings.going_out_deadwood_count)
        elif isinstance(action, DiscardAction):
            self.round.discard(action)
        elif isinstance(action, KnockAction):
            self.round.knock(action)
        else:
            raise Exception('Unknown step action={}'.format(action))
        self.actions.append(action)
        next_player_id = self.round.current_player_id
        next_state = self.get_state(player_id=next_player_id)
        return next_state, next_player_id

    def step_back(self):
        ''' Takes one step backward and restore to the last state
        '''
        raise NotImplementedError

    def get_num_players(self):
        ''' Return the number of players in the game
        '''
        return 2

    def get_num_actions(self):
        ''' Return the number of possible actions in the game
        '''
        return ActionEvent.get_num_actions()

    def get_player_id(self):
        ''' Return the current player that will take actions soon
        '''
        return self.round.current_player_id

    def is_over(self):
        ''' Return whether the current game is over
        '''
        return self.round.is_over

    def get_current_player(self) -> Player or None:
        return self.round.get_current_player()

    def get_last_action(self) -> ActionEvent or None:
        return self.actions[-1] if self.actions and len(self.actions) > 0 else None

    def get_state(self, player_id: int):
        ''' Get player's state

        Return:
            state (dict): The information of the state
        '''
        state = {}
        if not self.is_over():
            discard_pile = self.round.dealer.discard_pile
            top_discard = [] if not discard_pile else [discard_pile[-1]]
            dead_cards = discard_pile[:-1]
            last_action = self.get_last_action()
            opponent_id = (player_id + 1) % 2
            opponent = self.round.players[opponent_id]
            known_cards = opponent.known_cards
            if isinstance(last_action, ScoreNorthPlayerAction) or isinstance(last_action, ScoreSouthPlayerAction):
                known_cards = opponent.hand
            unknown_cards = self.round.dealer.stock_pile + [card for card in opponent.hand if card not in known_cards]
            state['player_id'] = self.round.current_player_id
            state['hand'] = [x.get_index() for x in self.round.players[self.round.current_player_id].hand]
            state['top_discard'] = [x.get_index() for x in top_discard]
            state['dead_cards'] = [x.get_index() for x in dead_cards]
            state['opponent_known_cards'] = [x.get_index() for x in known_cards]
            state['unknown_cards'] = [x.get_index() for x in unknown_cards]
        return state

    @staticmethod
    def decode_action(action_id) -> ActionEvent:  # FIXME 200213 should return str
        ''' Action id -> the action_event in the game.

        Args:
            action_id (int): the id of the action

        Returns:
            action (ActionEvent): the action that will be passed to the game engine.
        '''
        return ActionEvent.decode_action(action_id=action_id)


'''
gin_rummy/utils.py
'''
from typing import List, Iterable

import numpy as np

from gin_rummy import Card

from gin_rummy_error import GinRummyProgramError

valid_rank = ['A', '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K']
valid_suit = ['S', 'H', 'D', 'C']

rank_to_deadwood_value = {"A": 1, "2": 2, "3": 3, "4": 4, "5": 5, "6": 6, "7": 7, "8": 8, "9": 9,
                          "T": 10, "J": 10, "Q": 10, "K": 10}


def card_from_card_id(card_id: int) -> Card:
    ''' Make card from its card_id

    Args:
        card_id: int in range(0, 52)
     '''
    if not (0 <= card_id < 52):
        raise GinRummyProgramError("card_id is {}: should be 0 <= card_id < 52.".format(card_id))
    rank_id = card_id % 13
    suit_id = card_id // 13
    rank = Card.valid_rank[rank_id]
    suit = Card.valid_suit[suit_id]
    return Card(rank=rank, suit=suit)


# deck is always in order from AS, 2S, ..., AH, 2H, ..., AD, 2D, ..., AC, 2C, ... QC, KC
_deck = [card_from_card_id(card_id) for card_id in range(52)]  # want this to be read-only


def card_from_text(text: str) -> Card:
    if len(text) != 2:
        raise GinRummyProgramError("len(text) is {}: should be 2.".format(len(text)))
    return Card(rank=text[0], suit=text[1])


def get_deck() -> List[Card]:
    return _deck.copy()


def get_card(card_id: int):
    return _deck[card_id]


def get_card_id(card: Card) -> int:
    rank_id = get_rank_id(card)
    suit_id = get_suit_id(card)
    return rank_id + 13 * suit_id


def get_rank_id(card: Card) -> int:
    return Card.valid_rank.index(card.rank)


def get_suit_id(card: Card) -> int:
    return Card.valid_suit.index(card.suit)


def get_deadwood_value(card: Card) -> int:
    rank = card.rank
    deadwood_value = rank_to_deadwood_value.get(rank, 10)  # default to 10 is key does not exist
    return deadwood_value


def get_deadwood(hand: Iterable[Card], meld_cluster: List[Iterable[Card]]) -> List[Card]:
    if len(list(hand)) != 10:
        raise GinRummyProgramError("Hand contain {} cards: should be 10 cards.".format(len(list(hand))))
    meld_cards = [card for meld_pile in meld_cluster for card in meld_pile]
    deadwood = [card for card in hand if card not in meld_cards]
    return deadwood


def get_deadwood_count(hand: List[Card], meld_cluster: List[Iterable[Card]]) -> int:
    if len(hand) != 10:
        raise GinRummyProgramError("Hand contain {} cards: should be 10 cards.".format(len(hand)))
    deadwood = get_deadwood(hand=hand, meld_cluster=meld_cluster)
    deadwood_values = [get_deadwood_value(card) for card in deadwood]
    return sum(deadwood_values)


def decode_cards(env_cards: np.ndarray) -> List[Card]:
    result = []  # type: List[Card]
    if len(env_cards) != 52:
        raise GinRummyProgramError("len(env_cards) is {}: should be 52.".format(len(env_cards)))
    for i in range(52):
        if env_cards[i] == 1:
            card = _deck[i]
            result.append(card)
    return result


def encode_cards(cards: List[Card]) -> np.ndarray:
    plane = np.zeros(52, dtype=int)
    for card in cards:
        card_id = get_card_id(card)
        plane[card_id] = 1
    return plane


'''
gin_rummy/__init__.py
'''

from gin_rummy.base import Card as Card
from gin_rummy.player import GinRummyPlayer as Player
from gin_rummy.dealer import GinRummyDealer as Dealer
from gin_rummy.judge import GinRummyJudge as Judger


from gin_rummy.game import GinRummyGame as Game

'''
gin_rummy/melding.py
'''
from typing import List

from gin_rummy import Card

import utils
from gin_rummy_error import GinRummyProgramError

# ===============================================================
#    Terminology:
#        run_meld - three or more cards of same suit in sequence
#        set_meld - three or more cards of same rank
#        meld_pile - a run_meld or a set_meld
#        meld_piles - a list of meld_pile
#        meld_cluster - same as meld_piles, but usually with the piles being mutually disjoint
#        meld_clusters - a list of meld_cluster
# ===============================================================


def get_meld_clusters(hand: List[Card]) -> List[List[List[Card]]]:
    result = []  # type: List[List[List[Card]]]
    all_run_melds = [frozenset(x) for x in get_all_run_melds(hand)]
    all_set_melds = [frozenset(x) for x in get_all_set_melds(hand)]
    all_melds = all_run_melds + all_set_melds
    all_melds_count = len(all_melds)
    for i in range(0, all_melds_count):
        first_meld = all_melds[i]
        first_meld_list = list(first_meld)
        meld_cluster_1 = [first_meld_list]
        result.append(meld_cluster_1)
        for j in range(i + 1, all_melds_count):
            second_meld = all_melds[j]
            second_meld_list = list(second_meld)
            if not second_meld.isdisjoint(first_meld):
                continue
            meld_cluster_2 = [first_meld_list, second_meld_list]
            result.append(meld_cluster_2)
            for k in range(j + 1, all_melds_count):
                third_meld = all_melds[k]
                third_meld_list = list(third_meld)
                if not third_meld.isdisjoint(first_meld) or not third_meld.isdisjoint(second_meld):
                    continue
                meld_cluster_3 = [first_meld_list, second_meld_list, third_meld_list]
                result.append(meld_cluster_3)
    return result


def get_best_meld_clusters(hand: List[Card]) -> List[List[List[Card]]]:
    if len(hand) != 10:
        raise GinRummyProgramError("Hand contain {} cards: should be 10 cards.".format(len(hand)))
    result = []  # type: List[List[List[Card]]]
    meld_clusters = get_meld_clusters(hand=hand)  # type: List[List[List[Card]]]
    meld_clusters_count = len(meld_clusters)
    if meld_clusters_count > 0:
        deadwood_counts = [utils.get_deadwood_count(hand=hand, meld_cluster=meld_cluster)
                           for meld_cluster in meld_clusters]
        best_deadwood_count = min(deadwood_counts)
        for i in range(meld_clusters_count):
            if deadwood_counts[i] == best_deadwood_count:
                result.append(meld_clusters[i])
    return result


def get_all_run_melds(hand: List[Card]) -> List[List[Card]]:
    card_count = len(hand)
    hand_by_suit = sorted(hand, key=utils.get_card_id)
    max_run_melds = []

    i = 0
    while i < card_count - 2:
        card_i = hand_by_suit[i]
        j = i + 1
        card_j = hand_by_suit[j]
        while utils.get_rank_id(card_j) == utils.get_rank_id(card_i) + j - i and card_j.suit == card_i.suit:
            j += 1
            if j < card_count:
                card_j = hand_by_suit[j]
            else:
                break
        max_run_meld = hand_by_suit[i:j]
        if len(max_run_meld) >= 3:
            max_run_melds.append(max_run_meld)
        i = j

    result = []
    for max_run_meld in max_run_melds:
        max_run_meld_count = len(max_run_meld)
        for i in range(max_run_meld_count - 2):
            for j in range(i + 3, max_run_meld_count + 1):
                result.append(max_run_meld[i:j])
    return result


def get_all_set_melds(hand: List[Card]) -> List[List[Card]]:
    max_set_melds = []
    hand_by_rank = sorted(hand, key=lambda x: x.rank)
    set_meld = []
    current_rank = None
    for card in hand_by_rank:
        if current_rank is None or current_rank == card.rank:
            set_meld.append(card)
        else:
            if len(set_meld) >= 3:
                max_set_melds.append(set_meld)
            set_meld = [card]
        current_rank = card.rank
    if len(set_meld) >= 3:
        max_set_melds.append(set_meld)
    result = []
    for max_set_meld in max_set_melds:
        result.append(max_set_meld)
        if len(max_set_meld) == 4:
            for meld_card in max_set_meld:
                result.append([card for card in max_set_meld if card != meld_card])
    return result


def get_all_run_melds_for_suit(cards: List[Card], suit: str) -> List[List[Card]]:
    cards_for_suit = [card for card in cards if card.suit == suit]
    cards_for_suit_count = len(cards_for_suit)
    cards_for_suit = sorted(cards_for_suit, key=utils.get_card_id)
    max_run_melds = []

    i = 0
    while i < cards_for_suit_count - 2:
        card_i = cards_for_suit[i]
        j = i + 1
        card_j = cards_for_suit[j]
        while utils.get_rank_id(card_j) == utils.get_rank_id(card_i) + j - i:
            j += 1
            if j < cards_for_suit_count:
                card_j = cards_for_suit[j]
            else:
                break
        max_run_meld = cards_for_suit[i:j]
        if len(max_run_meld) >= 3:
            max_run_melds.append(max_run_meld)
        i = j

    result = []
    for max_run_meld in max_run_melds:
        max_run_meld_count = len(max_run_meld)
        for i in range(max_run_meld_count - 2):
            for j in range(i + 3, max_run_meld_count + 1):
                result.append(max_run_meld[i:j])
    return result


'''
gin_rummy/round.py
'''
from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from move import GinRummyMove

from typing import List

from gin_rummy import Dealer

from action_event import DrawCardAction, PickUpDiscardAction, DeclareDeadHandAction
from action_event import DiscardAction, KnockAction, GinAction
from action_event import ScoreNorthPlayerAction, ScoreSouthPlayerAction

from move import DealHandMove
from move import DrawCardMove, PickupDiscardMove, DeclareDeadHandMove
from move import DiscardMove, KnockMove, GinMove
from move import ScoreNorthMove, ScoreSouthMove

from gin_rummy_error import GinRummyProgramError

from gin_rummy import Player
import judge

import melding
import utils


class GinRummyRound:

    def __init__(self, dealer_id: int, np_random):
        ''' Initialize the round class

            The round class maintains the following instances:
                1) dealer: the dealer of the round; dealer has stock_pile and discard_pile
                2) players: the players in the round; each player has his own hand_pile
                3) current_player_id: the id of the current player who has the move
                4) is_over: true if the round is over
                5) going_out_action: knock or gin or None
                6) going_out_player_id: id of player who went out or None
                7) move_sheet: history of the moves of the player (including the deal_hand_move)

            The round class maintains a list of moves made by the players in self.move_sheet.
            move_sheet is similar to a chess score sheet.
            I didn't want to call it a score_sheet since it is not keeping score.
            I could have called move_sheet just moves, but that might conflict with the name moves used elsewhere.
            I settled on the longer name "move_sheet" to indicate that it is the official list of moves being made.

        Args:
            dealer_id: int
        '''
        self.np_random = np_random
        self.dealer_id = dealer_id
        self.dealer = Dealer(self.np_random)
        self.players = [Player(player_id=0, np_random=self.np_random), Player(player_id=1, np_random=self.np_random)]
        self.current_player_id = (dealer_id + 1) % 2
        self.is_over = False
        self.going_out_action = None  # going_out_action: int or None
        self.going_out_player_id = None  # going_out_player_id: int or None
        self.move_sheet = []  # type: List[GinRummyMove]
        player_dealing = Player(player_id=dealer_id, np_random=self.np_random)
        shuffled_deck = self.dealer.shuffled_deck
        self.move_sheet.append(DealHandMove(player_dealing=player_dealing, shuffled_deck=shuffled_deck))

    def get_current_player(self) -> Player or None:
        current_player_id = self.current_player_id
        return None if current_player_id is None else self.players[current_player_id]

    def draw_card(self, action: DrawCardAction):
        # when current_player takes DrawCardAction step, the move is recorded and executed
        # current_player keeps turn
        current_player = self.players[self.current_player_id]
        if not len(current_player.hand) == 10:
            raise GinRummyProgramError("len(current_player.hand) is {}: should be 10.".format(len(current_player.hand)))
        card = self.dealer.stock_pile.pop()
        self.move_sheet.append(DrawCardMove(current_player, action=action, card=card))
        current_player.add_card_to_hand(card=card)

    def pick_up_discard(self, action: PickUpDiscardAction):
        # when current_player takes PickUpDiscardAction step, the move is recorded and executed
        # opponent knows that the card is in current_player hand
        # current_player keeps turn
        current_player = self.players[self.current_player_id]
        if not len(current_player.hand) == 10:
            raise GinRummyProgramError("len(current_player.hand) is {}: should be 10.".format(len(current_player.hand)))
        card = self.dealer.discard_pile.pop()
        self.move_sheet.append(PickupDiscardMove(current_player, action, card=card))
        current_player.add_card_to_hand(card=card)
        current_player.known_cards.append(card)

    def declare_dead_hand(self, action: DeclareDeadHandAction):
        # when current_player takes DeclareDeadHandAction step, the move is recorded and executed
        # north becomes current_player to score his hand
        current_player = self.players[self.current_player_id]
        self.move_sheet.append(DeclareDeadHandMove(current_player, action))
        self.going_out_action = action
        self.going_out_player_id = self.current_player_id
        if not len(current_player.hand) == 10:
            raise GinRummyProgramError("len(current_player.hand) is {}: should be 10.".format(len(current_player.hand)))
        self.current_player_id = 0

    def discard(self, action: DiscardAction):
        # when current_player takes DiscardAction step, the move is recorded and executed
        # opponent knows that the card is no longer in current_player hand
        # current_player loses his turn and the opponent becomes the current player
        current_player = self.players[self.current_player_id]
        if not len(current_player.hand) == 11:
            raise GinRummyProgramError("len(current_player.hand) is {}: should be 11.".format(len(current_player.hand)))
        self.move_sheet.append(DiscardMove(current_player, action))
        card = action.card
        current_player.remove_card_from_hand(card=card)
        if card in current_player.known_cards:
            current_player.known_cards.remove(card)
        self.dealer.discard_pile.append(card)
        self.current_player_id = (self.current_player_id + 1) % 2

    def knock(self, action: KnockAction):
        # when current_player takes KnockAction step, the move is recorded and executed
        # opponent knows that the card is no longer in current_player hand
        # north becomes current_player to score his hand
        current_player = self.players[self.current_player_id]
        self.move_sheet.append(KnockMove(current_player, action))
        self.going_out_action = action
        self.going_out_player_id = self.current_player_id
        if not len(current_player.hand) == 11:
            raise GinRummyProgramError("len(current_player.hand) is {}: should be 11.".format(len(current_player.hand)))
        card = action.card
        current_player.remove_card_from_hand(card=card)
        if card in current_player.known_cards:
            current_player.known_cards.remove(card)
        self.current_player_id = 0

    def gin(self, action: GinAction, going_out_deadwood_count: int):
        # when current_player takes GinAction step, the move is recorded and executed
        # opponent knows that the card is no longer in current_player hand
        # north becomes current_player to score his hand
        current_player = self.players[self.current_player_id]
        self.move_sheet.append(GinMove(current_player, action))
        self.going_out_action = action
        self.going_out_player_id = self.current_player_id
        if not len(current_player.hand) == 11:
            raise GinRummyProgramError("len(current_player.hand) is {}: should be 11.".format(len(current_player.hand)))
        _, gin_cards = judge.get_going_out_cards(current_player.hand, going_out_deadwood_count)
        card = gin_cards[0]
        current_player.remove_card_from_hand(card=card)
        if card in current_player.known_cards:
            current_player.known_cards.remove(card)
        self.current_player_id = 0

    def score_player_0(self, action: ScoreNorthPlayerAction):
        # when current_player takes ScoreNorthPlayerAction step, the move is recorded and executed
        # south becomes current player
        if not self.current_player_id == 0:
            raise GinRummyProgramError("current_player_id is {}: should be 0.".format(self.current_player_id))
        current_player = self.get_current_player()
        best_meld_clusters = melding.get_best_meld_clusters(hand=current_player.hand)
        best_meld_cluster = [] if not best_meld_clusters else best_meld_clusters[0]
        deadwood_count = utils.get_deadwood_count(hand=current_player.hand, meld_cluster=best_meld_cluster)
        self.move_sheet.append(ScoreNorthMove(player=current_player,
                                              action=action,
                                              best_meld_cluster=best_meld_cluster,
                                              deadwood_count=deadwood_count))
        self.current_player_id = 1

    def score_player_1(self, action: ScoreSouthPlayerAction):
        # when current_player takes ScoreSouthPlayerAction step, the move is recorded and executed
        # south remains current player
        # the round is over
        if not self.current_player_id == 1:
            raise GinRummyProgramError("current_player_id is {}: should be 1.".format(self.current_player_id))
        current_player = self.get_current_player()
        best_meld_clusters = melding.get_best_meld_clusters(hand=current_player.hand)
        best_meld_cluster = [] if not best_meld_clusters else best_meld_clusters[0]
        deadwood_count = utils.get_deadwood_count(hand=current_player.hand, meld_cluster=best_meld_cluster)
        self.move_sheet.append(ScoreSouthMove(player=current_player,
                                              action=action,
                                              best_meld_cluster=best_meld_cluster,
                                              deadwood_count=deadwood_count))
        self.is_over = True


'''
gin_rummy/gin_rummy_error.py
'''
class GinRummyError(Exception):
    pass


class GinRummyProgramError(GinRummyError):
    pass


