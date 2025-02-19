from leducholdem.utils import init_standard_deck
from leducholdem import Card

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


class LeducholdemDealer(LimitHoldemDealer):

    def __init__(self, np_random):
        ''' Initialize a leducholdem dealer class
        '''
        self.np_random = np_random
        self.deck = [Card('S', 'J'), Card('H', 'J'), Card('S', 'Q'), Card('H', 'Q'), Card('S', 'K'), Card('H', 'K')]
        self.shuffle()
        self.pot = 0

