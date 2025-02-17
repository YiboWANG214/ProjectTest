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
