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



class NolimitholdemPlayer(LimitHoldemPlayer):
    def __init__(self, player_id, init_chips, np_random):
        """
        Initialize a player.

        Args:
            player_id (int): The id of the player
            init_chips (int): The number of chips the player has initially
        """
        super().__init__(player_id, np_random)
        self.remained_chips = init_chips

    def bet(self, chips):
        quantity = chips if chips <= self.remained_chips else self.remained_chips
        self.in_chips += quantity
        self.remained_chips -= quantity
