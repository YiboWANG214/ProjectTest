```python
# test_uno_round.py

import unittest
from uno.round import UnoRound
from uno.card import UnoCard

class TestUnoRound(unittest.TestCase):

    def setUp(self):
        self.round = UnoRound(None, 2, np_random=None)
        self.players = [UnoPlayer(0, np_random=None), UnoPlayer(1, np_random=None)]

    def test_flip_top_card(self):
        top_card = self.round.flip_top_card()
        self.assertIsInstance(top_card, UnoCard)

    def test_perform_top_card(self):
        top_card = UnoCard('number', 'r', '3')
        self.round.perform_top_card(self.players, top_card)
        self.assertEqual(self.round.current_player, 1)
        self.assertEqual(self.round.direction, 1)

    def test_proceed_round(self):
        top_card = UnoCard('number', 'r', '3')
        self.round.target = top_card
        self.round.current_player = 0
        self.round.proceed_round(self.players, 'r-3')
        self.assertEqual(self.round.current_player, 1)

    def test_get_legal_actions(self):
        top_card = UnoCard('number', 'r', '3')
        self.round.target = top_card
        self.players[0].hand = [UnoCard('number', 'r', '2')]
        legal_actions = self.round.get_legal_actions(self.players, 0)
        self.assertListEqual(legal_actions, ['r-2'])

    def test_get_state(self):
        top_card = UnoCard('number', 'r', '3')
        self.round.target = top_card
        self.players[0].hand = [UnoCard('number', 'r', '2')]
        state = self.round.get_state(self.players, 0)
        self.assertIn('hand', state)
        self.assertIn('target', state)
        self.assertIn('played_cards', state)
        self.assertIn('legal_actions', state)
        self.assertIn('num_cards', state)

if __name__ == '__main__':
    unittest.main()
```

```python
# test_uno_dealer.py

import unittest
from uno.dealer import UnoDealer

class TestUnoDealer(unittest.TestCase):

    def setUp(self):
        self.dealer = UnoDealer(np_random=None)

    def test_shuffle(self):
        deck_before = self.dealer.deck[:]
        self.dealer.shuffle()
        deck_after = self.dealer.deck
        self.assertNotEqual(deck_before, deck_after)

    def test_deal_cards(self):
        player = UnoPlayer(0, np_random=None)
        num = 3
        self.dealer.deal_cards(player, num)
        self.assertEqual(len(player.hand), num)

    def test_flip_top_card(self):
        top_card = self.dealer.flip_top_card()
        self.assertIsNotNone(top_card)

if __name__ == '__main__':
    unittest.main()
```

```python
# test_uno_player.py

import unittest
from uno.player import UnoPlayer

class TestUnoPlayer(unittest.TestCase):

    def setUp(self):
        self.player = UnoPlayer(0, np_random=None)

    def test_get_player_id(self):
        player_id = self.player.get_player_id()
        self.assertEqual(player_id, 0)

if __name__ == '__main__':
    unittest.main()
```

```python
# test_uno_game.py

import unittest
from uno.game import UnoGame

class TestUnoGame(unittest.TestCase):

    def setUp(self):
        self.game = UnoGame(allow_step_back=False, num_players=2)

    def test_configure(self):
        game_config = {'game_num_players': 2}
        self.game.configure(game_config)
        self.assertEqual(self.game.num_players, 2)

    def test_get_num_players(self):
        num_players = self.game.get_num_players()
        self.assertEqual(num_players, 2)

    def test_get_num_actions(self):
        num_actions = UnoGame.get_num_actions()
        self.assertEqual(num_actions, 61)

    def test_get_legal_actions(self):
        legal_actions = self.game.get_legal_actions()
        self.assertIsInstance(legal_actions, list)

if __name__ == '__main__':
    unittest.main()
```

```python
# test_uno_judger.py

import unittest
from uno.judger import UnoJudger

class TestUnoJudger(unittest.TestCase):

    def setUp(self):
        self.players = [UnoPlayer(0, np_random=None), UnoPlayer(1, np_random=None)]

    def test_judge_winner(self):
        np_random = None
        winner = UnoJudger.judge_winner(self.players, np_random)
        self.assertIsInstance(winner, list)

if __name__ == '__main__':
    unittest.main()
```

```python
# test_uno_card.py

import unittest
from uno.card import UnoCard

class TestUnoCard(unittest.TestCase):

    def test_get_str(self):
        card = UnoCard('number', 'r', '3')
        card_str = card.get_str()
        self.assertEqual(card_str, 'r-3')

    def test_print_cards(self):
        cards = ['r-0', 'g-skip']
        UnoCard.print_cards(cards, wild_color=False)

if __name__ == '__main__':
    unittest.main()
```

```python
# test_uno_utils.py

import unittest
from uno.utils import init_deck, cards2list, hand2dict, encode_hand, encode_target

class TestUtils(unittest.TestCase):

    def test_init_deck(self):
        deck = init_deck()
        self.assertEqual(len(deck), 108)

    def test_cards2list(self):
        hand = ['r-0', 'g-3']
        cards_list = cards2list(hand)
        self.assertIsInstance(cards_list, list)

    def test_hand2dict(self):
        hand = ['r-0', 'g-3']
        hand_dict = hand2dict(hand)
        self.assertIsInstance(hand_dict, dict)

    def test_encode_hand(self):
        hand = ['r-0', 'g-3']
        plane = encode_hand(np.zeros((3, 4, 15), dtype=int), hand)
        self.assertIsInstance(plane, np.ndarray)

    def test_encode_target(self):
        target = 'r-0'
        plane = encode_target(np.zeros((1, 4, 15), dtype=int), target)
        self.assertIsInstance(plane, np.ndarray)

if __name__ == '__main__':
    unittest.main()
```

```python
# test_uno_init.py

import unittest
from uno import Dealer, Judger, Player, Round, Game

class TestUnoInit(unittest.TestCase):

    def test_imports(self):
        self.assertTrue(Dealer)
        self.assertTrue(Judger)
        self.assertTrue(Player)
        self.assertTrue(Round)
        self.assertTrue(Game)

if __name__ == '__main__':
    unittest.main()
```