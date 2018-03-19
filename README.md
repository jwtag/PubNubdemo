# PubNubdemo
TL;DR: A picture guessing game using the PubNub SDK and the Google Custom Search Engine API.

For my demo, I took the PubNub SDK and created a simple game. 
In my game, There are two players: the guesser and the word-provider. 
The first person to connect to the game’s channel is the word-provider, 
and the second person to connect to the channel is the guesser.

The game works as follows:

The word-provider types in a word of his choosing and then hits the “send” button. 
This word is then sent into Google Images, and on both the word provider’s screen and the guesser’s screen the 
first Google Images result pops up.  The guesser then has to guess what term the word-provider typed in to 
get the image they received. They do this by sending messages to the word provider by typing in the text box and then hitting “send”.
After the word-provider sent their word off to the guesser, their text entry field disappears and is replaced by a pair of 
"Yes” and “No” buttons. Based upon the feedback that the word-provider receives from the guesser, they can respond with by 
hitting either of the buttons to send back the respective response. Note that the word-provider cannot give any hints: they can 
only say “yes” or “no.”  Once the guesser finally gets the answer correct, the word-provider hits the “yes” button. 
This causes the players to switch roles and the game restarts.

NOTE: Since I used the free version of the Google Custom Search API as my backend for finding the image on Google Images, I only have about 100 (I think) searches available per day. Once that quota is exceeded, my program breaks since I’d have to pay to keep it working.
