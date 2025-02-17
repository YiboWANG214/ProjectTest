'''
bridge/base.py
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
bridge/player.py
'''
from typing import List

from bridge_card import BridgeCard


class BridgePlayer:

    def __init__(self, player_id: int, np_random):
        ''' Initialize a BridgePlayer player class

        Args:
            player_id (int): id for the player
        '''
        if player_id < 0 or player_id > 3:
            raise Exception(f'BridgePlayer has invalid player_id: {player_id}')
        self.np_random = np_random
        self.player_id: int = player_id
        self.hand: List[BridgeCard] = []

    def remove_card_from_hand(self, card: BridgeCard):
        self.hand.remove(card)

    def __str__(self):
        return ['N', 'E', 'S', 'W'][self.player_id]


'''
bridge/move.py
'''
#
#   These classes are used to keep a move_sheet history of the moves in a round.
#

from action_event import ActionEvent, BidAction, PassAction, DblAction, RdblAction, PlayCardAction
from bridge_card import BridgeCard

from bridge import Player


class BridgeMove(object):  # Interface
    pass


class PlayerMove(BridgeMove):  # Interface

    def __init__(self, player: Player, action: ActionEvent):
        super().__init__()
        self.player = player
        self.action = action


class CallMove(PlayerMove):  # Interface

    def __init__(self, player: Player, action: ActionEvent):
        super().__init__(player=player, action=action)


class DealHandMove(BridgeMove):

    def __init__(self, dealer: Player, shuffled_deck: [BridgeCard]):
        super().__init__()
        self.dealer = dealer
        self.shuffled_deck = shuffled_deck

    def __str__(self):
        shuffled_deck_text = " ".join([str(card) for card in self.shuffled_deck])
        return f'{self.dealer} deal shuffled_deck=[{shuffled_deck_text}]'


class MakePassMove(CallMove):

    def __init__(self, player: Player):
        super().__init__(player=player, action=PassAction())

    def __str__(self):
        return f'{self.player} {self.action}'


class MakeDblMove(CallMove):

    def __init__(self, player: Player):
        super().__init__(player=player, action=DblAction())

    def __str__(self):
        return f'{self.player} {self.action}'


class MakeRdblMove(CallMove):

    def __init__(self, player: Player):
        super().__init__(player=player, action=RdblAction())

    def __str__(self):
        return f'{self.player} {self.action}'


class MakeBidMove(CallMove):

    def __init__(self, player: Player, bid_action: BidAction):
        super().__init__(player=player, action=bid_action)
        self.action = bid_action  # Note: keep type as BidAction rather than ActionEvent

    def __str__(self):
        return f'{self.player} bids {self.action}'


class PlayCardMove(PlayerMove):

    def __init__(self, player: Player, action: PlayCardAction):
        super().__init__(player=player, action=action)
        self.action = action  # Note: keep type as PlayCardAction rather than ActionEvent

    @property
    def card(self):
        return self.action.card

    def __str__(self):
        return f'{self.player} plays {self.action}'


'''
bridge/dealer.py
'''
from typing import List

from bridge import Player
from bridge_card import BridgeCard


class BridgeDealer:
    ''' Initialize a BridgeDealer dealer class
    '''
    def __init__(self, np_random):
        ''' set shuffled_deck, set stock_pile
        '''
        self.np_random = np_random
        self.shuffled_deck: List[BridgeCard] = BridgeCard.get_deck()  # keep a copy of the shuffled cards at start of new hand
        self.np_random.shuffle(self.shuffled_deck)
        self.stock_pile: List[BridgeCard] = self.shuffled_deck.copy()

    def deal_cards(self, player: Player, num: int):
        ''' Deal some cards from stock_pile to one player

        Args:
            player (BridgePlayer): The BridgePlayer object
            num (int): The number of cards to be dealt
        '''
        for _ in range(num):
            player.hand.append(self.stock_pile.pop())


'''
bridge/judger.py
'''
from typing import List

from typing import TYPE_CHECKING

from action_event import PlayCardAction
from action_event import ActionEvent, BidAction, PassAction, DblAction, RdblAction
from move import MakeBidMove, MakeDblMove, MakeRdblMove
from bridge_card import BridgeCard


class BridgeJudger:

    '''
        Judger decides legal actions for current player
    '''

    def __init__(self, game: 'BridgeGame'):
        ''' Initialize the class BridgeJudger
        :param game: BridgeGame
        '''
        self.game: BridgeGame = game

    def get_legal_actions(self) -> List[ActionEvent]:
        """
        :return: List[ActionEvent] of legal actions
        """
        legal_actions: List[ActionEvent] = []
        if not self.game.is_over():
            current_player = self.game.round.get_current_player()
            if not self.game.round.is_bidding_over():
                legal_actions.append(PassAction())
                last_make_bid_move: MakeBidMove or None = None
                last_dbl_move: MakeDblMove or None = None
                last_rdbl_move: MakeRdblMove or None = None
                for move in reversed(self.game.round.move_sheet):
                    if isinstance(move, MakeBidMove):
                        last_make_bid_move = move
                        break
                    elif isinstance(move, MakeRdblMove):
                        last_rdbl_move = move
                    elif isinstance(move, MakeDblMove) and not last_rdbl_move:
                        last_dbl_move = move
                first_bid_action_id = ActionEvent.first_bid_action_id
                next_bid_action_id = last_make_bid_move.action.action_id + 1 if last_make_bid_move else first_bid_action_id
                for bid_action_id in range(next_bid_action_id, first_bid_action_id + 35):
                    action = BidAction.from_action_id(action_id=bid_action_id)
                    legal_actions.append(action)
                if last_make_bid_move and last_make_bid_move.player.player_id % 2 != current_player.player_id % 2 and not last_dbl_move and not last_rdbl_move:
                    legal_actions.append(DblAction())
                if last_dbl_move and last_dbl_move.player.player_id % 2 != current_player.player_id % 2:
                    legal_actions.append(RdblAction())
            else:
                trick_moves = self.game.round.get_trick_moves()
                hand = self.game.round.players[current_player.player_id].hand
                legal_cards = hand
                if trick_moves and len(trick_moves) < 4:
                    led_card: BridgeCard = trick_moves[0].card
                    cards_of_led_suit = [card for card in hand if card.suit == led_card.suit]
                    if cards_of_led_suit:
                        legal_cards = cards_of_led_suit
                for card in legal_cards:
                    action = PlayCardAction(card=card)
                    legal_actions.append(action)
        return legal_actions


'''
bridge/action_event.py
'''
from bridge_card import BridgeCard

# ====================================
# Action_ids:
#       0 -> no_bid_action_id
#       1 to 35 -> bid_action_id (bid amount by suit or NT)
#       36 -> pass_action_id
#       37 -> dbl_action_id
#       38 -> rdbl_action_id
#       39 to 90 -> play_card_action_id
# ====================================


class ActionEvent(object):  # Interface

    no_bid_action_id = 0
    first_bid_action_id = 1
    pass_action_id = 36
    dbl_action_id = 37
    rdbl_action_id = 38
    first_play_card_action_id = 39

    def __init__(self, action_id: int):
        self.action_id = action_id

    def __eq__(self, other):
        result = False
        if isinstance(other, ActionEvent):
            result = self.action_id == other.action_id
        return result

    @staticmethod
    def from_action_id(action_id: int):
        if action_id == ActionEvent.pass_action_id:
            return PassAction()
        elif ActionEvent.first_bid_action_id <= action_id <= 35:
            bid_amount = 1 + (action_id - ActionEvent.first_bid_action_id) // 5
            bid_suit_id = (action_id - ActionEvent.first_bid_action_id) % 5
            bid_suit = BridgeCard.suits[bid_suit_id] if bid_suit_id < 4 else None
            return BidAction(bid_amount, bid_suit)
        elif action_id == ActionEvent.dbl_action_id:
            return DblAction()
        elif action_id == ActionEvent.rdbl_action_id:
            return RdblAction()
        elif ActionEvent.first_play_card_action_id <= action_id < ActionEvent.first_play_card_action_id + 52:
            card_id = action_id - ActionEvent.first_play_card_action_id
            card = BridgeCard.card(card_id=card_id)
            return PlayCardAction(card=card)
        else:
            raise Exception(f'ActionEvent from_action_id: invalid action_id={action_id}')

    @staticmethod
    def get_num_actions():
        ''' Return the number of possible actions in the game
        '''
        return 1 + 35 + 3 + 52  # no_bid, 35 bids, pass, dbl, rdl, 52 play_card


class CallActionEvent(ActionEvent):  # Interface
    pass


class PassAction(CallActionEvent):

    def __init__(self):
        super().__init__(action_id=ActionEvent.pass_action_id)

    def __str__(self):
        return "pass"

    def __repr__(self):
        return "pass"


class BidAction(CallActionEvent):

    def __init__(self, bid_amount: int, bid_suit: str or None):
        suits = BridgeCard.suits
        if bid_suit and bid_suit not in suits:
            raise Exception(f'BidAction has invalid suit: {bid_suit}')
        if bid_suit in suits:
            bid_suit_id = suits.index(bid_suit)
        else:
            bid_suit_id = 4
        bid_action_id = bid_suit_id + 5 * (bid_amount - 1) + ActionEvent.first_bid_action_id
        super().__init__(action_id=bid_action_id)
        self.bid_amount = bid_amount
        self.bid_suit = bid_suit

    def __str__(self):
        bid_suit = self.bid_suit
        if not bid_suit:
            bid_suit = 'NT'
        return f'{self.bid_amount}{bid_suit}'

    def __repr__(self):
        return self.__str__()


class DblAction(CallActionEvent):

    def __init__(self):
        super().__init__(action_id=ActionEvent.dbl_action_id)

    def __str__(self):
        return "dbl"

    def __repr__(self):
        return "dbl"


class RdblAction(CallActionEvent):

    def __init__(self):
        super().__init__(action_id=ActionEvent.rdbl_action_id)

    def __str__(self):
        return "rdbl"

    def __repr__(self):
        return "rdbl"


class PlayCardAction(ActionEvent):

    def __init__(self, card: BridgeCard):
        play_card_action_id = ActionEvent.first_play_card_action_id + card.card_id
        super().__init__(action_id=play_card_action_id)
        self.card: BridgeCard = card

    def __str__(self):
        return f"{self.card}"

    def __repr__(self):
        return f"{self.card}"


'''
bridge/tray.py
'''
class Tray(object):

    def __init__(self, board_id: int):
        if board_id <= 0:
            raise Exception(f'Tray: invalid board_id={board_id}')
        self.board_id = board_id

    @property
    def dealer_id(self):
        return (self.board_id - 1) % 4

    @property
    def vul(self):
        vul_none = [0, 0, 0, 0]
        vul_n_s = [1, 0, 1, 0]
        vul_e_w = [0, 1, 0, 1]
        vul_all = [1, 1, 1, 1]
        basic_vuls = [vul_none, vul_n_s, vul_e_w, vul_all]
        offset = (self.board_id - 1) // 4
        return basic_vuls[(self.board_id - 1 + offset) % 4]

    def __str__(self):
        return f'{self.board_id}: dealer_id={self.dealer_id} vul={self.vul}'


'''
bridge/game.py
'''
from typing import List

import numpy as np

from bridge import Judger
from round import BridgeRound
from action_event import ActionEvent, CallActionEvent, PlayCardAction


class BridgeGame:
    ''' Game class. This class will interact with outer environment.
    '''

    def __init__(self, allow_step_back=False):
        '''Initialize the class BridgeGame
        '''
        self.allow_step_back: bool = allow_step_back
        self.np_random = np.random.RandomState()
        self.judger: Judger = Judger(game=self)
        self.actions: [ActionEvent] = []  # must reset in init_game
        self.round: BridgeRound or None = None  # must reset in init_game
        self.num_players: int = 4

    def init_game(self):
        ''' Initialize all characters in the game and start round 1
        '''
        board_id = self.np_random.choice([1, 2, 3, 4])
        self.actions: List[ActionEvent] = []
        self.round = BridgeRound(num_players=self.num_players, board_id=board_id, np_random=self.np_random)
        for player_id in range(4):
            player = self.round.players[player_id]
            self.round.dealer.deal_cards(player=player, num=13)
        current_player_id = self.round.current_player_id
        state = self.get_state(player_id=current_player_id)
        return state, current_player_id

    def step(self, action: ActionEvent):
        ''' Perform game action and return next player number, and the state for next player
        '''
        if isinstance(action, CallActionEvent):
            self.round.make_call(action=action)
        elif isinstance(action, PlayCardAction):
            self.round.play_card(action=action)
        else:
            raise Exception(f'Unknown step action={action}')
        self.actions.append(action)
        next_player_id = self.round.current_player_id
        next_state = self.get_state(player_id=next_player_id)
        return next_state, next_player_id

    def get_num_players(self) -> int:
        ''' Return the number of players in the game
        '''
        return self.num_players

    @staticmethod
    def get_num_actions() -> int:
        ''' Return the number of possible actions in the game
        '''
        return ActionEvent.get_num_actions()

    def get_player_id(self):
        ''' Return the current player that will take actions soon
        '''
        return self.round.current_player_id

    def is_over(self) -> bool:
        ''' Return whether the current game is over
        '''
        return self.round.is_over()

    def get_state(self, player_id: int):  # wch: not really used
        ''' Get player's state

        Return:
            state (dict): The information of the state
        '''
        state = {}
        if not self.is_over():
            state['player_id'] = player_id
            state['current_player_id'] = self.round.current_player_id
            state['hand'] = self.round.players[player_id].hand
        else:
            state['player_id'] = player_id
            state['current_player_id'] = self.round.current_player_id
            state['hand'] = self.round.players[player_id].hand
        return state


'''
bridge/__init__.py
'''
from bridge.base import Card as Card
from bridge.player import BridgePlayer as Player
from bridge.dealer import BridgeDealer as Dealer
from bridge.judger import BridgeJudger as Judger
from bridge.game import BridgeGame as Game



'''
bridge/round.py
'''
from typing import List

from bridge import Dealer
from bridge import Player

from action_event import CallActionEvent, PassAction, DblAction, RdblAction, BidAction, PlayCardAction
from move import BridgeMove, DealHandMove, PlayCardMove, MakeBidMove, MakePassMove, MakeDblMove, MakeRdblMove, CallMove
from tray import Tray


class BridgeRound:

    @property
    def dealer_id(self) -> int:
        return self.tray.dealer_id

    @property
    def vul(self):
        return self.tray.vul

    @property
    def board_id(self) -> int:
        return self.tray.board_id

    @property
    def round_phase(self):
        if self.is_over():
            result = 'game over'
        elif self.is_bidding_over():
            result = 'play card'
        else:
            result = 'make bid'
        return result

    def __init__(self, num_players: int, board_id: int, np_random):
        ''' Initialize the round class

            The round class maintains the following instances:
                1) dealer: the dealer of the round; dealer has trick_pile
                2) players: the players in the round; each player has his own hand_pile
                3) current_player_id: the id of the current player who has the move
                4) doubling_cube: 2 if contract is doubled; 4 if contract is redoubled; else 1
                5) play_card_count: count of PlayCardMoves
                5) move_sheet: history of the moves of the players (including the deal_hand_move)

            The round class maintains a list of moves made by the players in self.move_sheet.
            move_sheet is similar to a chess score sheet.
            I didn't want to call it a score_sheet since it is not keeping score.
            I could have called move_sheet just moves, but that might conflict with the name moves used elsewhere.
            I settled on the longer name "move_sheet" to indicate that it is the official list of moves being made.

        Args:
            num_players: int
            board_id: int
            np_random
        '''
        tray = Tray(board_id=board_id)
        dealer_id = tray.dealer_id
        self.tray = tray
        self.np_random = np_random
        self.dealer: Dealer = Dealer(self.np_random)
        self.players: List[Player] = []
        for player_id in range(num_players):
            self.players.append(Player(player_id=player_id, np_random=self.np_random))
        self.current_player_id: int = dealer_id
        self.doubling_cube: int = 1
        self.play_card_count: int = 0
        self.contract_bid_move: MakeBidMove or None = None
        self.won_trick_counts = [0, 0]  # count of won tricks by side
        self.move_sheet: List[BridgeMove] = []
        self.move_sheet.append(DealHandMove(dealer=self.players[dealer_id], shuffled_deck=self.dealer.shuffled_deck))

    def is_bidding_over(self) -> bool:
        ''' Return whether the current bidding is over
        '''
        is_bidding_over = True
        if len(self.move_sheet) < 5:
            is_bidding_over = False
        else:
            last_make_pass_moves: List[MakePassMove] = []
            for move in reversed(self.move_sheet):
                if isinstance(move, MakePassMove):
                    last_make_pass_moves.append(move)
                    if len(last_make_pass_moves) == 3:
                        break
                elif isinstance(move, CallMove):
                    is_bidding_over = False
                    break
                else:
                    break
        return is_bidding_over

    def is_over(self) -> bool:
        ''' Return whether the current game is over
        '''
        is_over = True
        if not self.is_bidding_over():
            is_over = False
        elif self.contract_bid_move:
            for player in self.players:
                if player.hand:
                    is_over = False
                    break
        return is_over

    def get_current_player(self) -> Player or None:
        current_player_id = self.current_player_id
        return None if current_player_id is None else self.players[current_player_id]

    def get_trick_moves(self) -> List[PlayCardMove]:
        trick_moves: List[PlayCardMove] = []
        if self.is_bidding_over():
            if self.play_card_count > 0:
                trick_pile_count = self.play_card_count % 4
                if trick_pile_count == 0:
                    trick_pile_count = 4  # wch: note this
                for move in self.move_sheet[-trick_pile_count:]:
                    if isinstance(move, PlayCardMove):
                        trick_moves.append(move)
                if len(trick_moves) != trick_pile_count:
                    raise Exception(f'get_trick_moves: count of trick_moves={[str(move.card) for move in trick_moves]} does not equal {trick_pile_count}')
        return trick_moves

    def get_trump_suit(self) -> str or None:
        trump_suit = None
        if self.contract_bid_move:
            trump_suit = self.contract_bid_move.action.bid_suit
        return trump_suit

    def make_call(self, action: CallActionEvent):
        # when current_player takes CallActionEvent step, the move is recorded and executed
        current_player = self.players[self.current_player_id]
        if isinstance(action, PassAction):
            self.move_sheet.append(MakePassMove(current_player))
        elif isinstance(action, BidAction):
            self.doubling_cube = 1
            make_bid_move = MakeBidMove(current_player, action)
            self.contract_bid_move = make_bid_move
            self.move_sheet.append(make_bid_move)
        elif isinstance(action, DblAction):
            self.doubling_cube = 2
            self.move_sheet.append(MakeDblMove(current_player))
        elif isinstance(action, RdblAction):
            self.doubling_cube = 4
            self.move_sheet.append(MakeRdblMove(current_player))
        if self.is_bidding_over():
            if not self.is_over():
                self.current_player_id = self.get_left_defender().player_id
        else:
            self.current_player_id = (self.current_player_id + 1) % 4

    def play_card(self, action: PlayCardAction):
        # when current_player takes PlayCardAction step, the move is recorded and executed
        current_player = self.players[self.current_player_id]
        self.move_sheet.append(PlayCardMove(current_player, action))
        card = action.card
        current_player.remove_card_from_hand(card=card)
        self.play_card_count += 1
        # update current_player_id
        trick_moves = self.get_trick_moves()
        if len(trick_moves) == 4:
            trump_suit = self.get_trump_suit()
            winning_card = trick_moves[0].card
            trick_winner = trick_moves[0].player
            for move in trick_moves[1:]:
                trick_card = move.card
                trick_player = move.player
                if trick_card.suit == winning_card.suit:
                    if trick_card.card_id > winning_card.card_id:
                        winning_card = trick_card
                        trick_winner = trick_player
                elif trick_card.suit == trump_suit:
                    winning_card = trick_card
                    trick_winner = trick_player
            self.current_player_id = trick_winner.player_id
            self.won_trick_counts[trick_winner.player_id % 2] += 1
        else:
            self.current_player_id = (self.current_player_id + 1) % 4

    def get_declarer(self) -> Player or None:
        declarer = None
        if self.contract_bid_move:
            trump_suit = self.contract_bid_move.action.bid_suit
            side = self.contract_bid_move.player.player_id % 2
            for move in self.move_sheet:
                if isinstance(move, MakeBidMove) and move.action.bid_suit == trump_suit and move.player.player_id % 2 == side:
                    declarer = move.player
                    break
        return declarer

    def get_dummy(self) -> Player or None:
        dummy = None
        declarer = self.get_declarer()
        if declarer:
            dummy = self.players[(declarer.player_id + 2) % 4]
        return dummy

    def get_left_defender(self) -> Player or None:
        left_defender = None
        declarer = self.get_declarer()
        if declarer:
            left_defender = self.players[(declarer.player_id + 1) % 4]
        return left_defender

    def get_right_defender(self) -> Player or None:
        right_defender = None
        declarer = self.get_declarer()
        if declarer:
            right_defender = self.players[(declarer.player_id + 3) % 4]
        return right_defender

    def get_perfect_information(self):
        state = {}
        last_call_move = None
        if not self.is_bidding_over() or self.play_card_count == 0:
            last_move = self.move_sheet[-1]
            if isinstance(last_move, CallMove):
                last_call_move = last_move
        trick_moves = [None, None, None, None]
        if self.is_bidding_over():
            for trick_move in self.get_trick_moves():
                trick_moves[trick_move.player.player_id] = trick_move.card
        state['move_count'] = len(self.move_sheet)
        state['tray'] = self.tray
        state['current_player_id'] = self.current_player_id
        state['round_phase'] = self.round_phase
        state['last_call_move'] = last_call_move
        state['doubling_cube'] = self.doubling_cube
        state['contact'] = self.contract_bid_move if self.is_bidding_over() and self.contract_bid_move else None
        state['hands'] = [player.hand for player in self.players]
        state['trick_moves'] = trick_moves
        return state

    def print_scene(self):
        print(f'===== Board: {self.tray.board_id} move: {len(self.move_sheet)} player: {self.players[self.current_player_id]} phase: {self.round_phase} =====')
        print(f'dealer={self.players[self.tray.dealer_id]}')
        print(f'vul={self.vul}')
        if not self.is_bidding_over() or self.play_card_count == 0:
            last_move = self.move_sheet[-1]
            last_call_text = f'{last_move}' if isinstance(last_move, CallMove) else 'None'
            print(f'last call: {last_call_text}')
        if self.is_bidding_over() and self.contract_bid_move:
            bid_suit = self.contract_bid_move.action.bid_suit
            doubling_cube = self.doubling_cube
            if not bid_suit:
                bid_suit = 'NT'
            doubling_cube_text = "" if doubling_cube == 1 else "dbl" if doubling_cube == 2 else "rdbl"
            print(f'contract: {self.contract_bid_move.player} {self.contract_bid_move.action.bid_amount}{bid_suit} {doubling_cube_text}')
        for player in self.players:
            print(f'{player}: {[str(card) for card in player.hand]}')
        if self.is_bidding_over():
            trick_pile = ['None', 'None', 'None', 'None']
            for trick_move in self.get_trick_moves():
                trick_pile[trick_move.player.player_id] = trick_move.card
            print(f'trick_pile: {[str(card) for card in trick_pile]}')


'''
bridge/bridge_card.py
'''
from bridge import Card


class BridgeCard(Card):

    suits = ['C', 'D', 'H', 'S']
    ranks = ['2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A']

    @staticmethod
    def card(card_id: int):
        return _deck[card_id]

    @staticmethod
    def get_deck() -> [Card]:
        return _deck.copy()

    def __init__(self, suit: str, rank: str):
        super().__init__(suit=suit, rank=rank)
        suit_index = BridgeCard.suits.index(self.suit)
        rank_index = BridgeCard.ranks.index(self.rank)
        self.card_id = 13 * suit_index + rank_index

    def __str__(self):
        return f'{self.rank}{self.suit}'

    def __repr__(self):
        return f'{self.rank}{self.suit}'


# deck is always in order from 2C, ... KC, AC, 2D, ... KD, AD, 2H, ... KH, AH, 2S, ... KS, AS
_deck = [BridgeCard(suit=suit, rank=rank) for suit in BridgeCard.suits for rank in BridgeCard.ranks]  # want this to be read-only


