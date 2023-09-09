# Building Systems with the ChatGPT API in Scala

This repo implements ideas from the [deeplearning.ai](https://www.deeplearning.ai) short course [Building Systems with the ChatGPT API](https://www.deeplearning.ai/short-courses/building-systems-with-chatgpt/) in Scala.

## JSON Output Parsing and Prompt Chaining

See [this example](src/main/scala/chain/Main.scala).

```scala
case class Person(
    first: String,
    last: String,
)

case class Story(
    title: String,
    text: String,
)

for {
  person <- llm.promptAs[Person](
    "Make up a person's first and last name. Return only a json object with first and last fields."
  )
  story <- llm.promptAs[Story](
    s"""You are a talented author of children's stories. Write a short story about ${person.first} ${person.last}.
         |Return only a json object with the following fields:
         |title: the title of your short story
         |text: the short story
         |Make sure you return only a valid json object with these fields.""".stripMargin
  )
} yield story
```

Here is an example story:

```
 The Adventures of John Doe
 --------
 Once upon a time in a small village, there lived a curious and adventurous boy named John Doe. With his bright eyes and friendly smile, he was loved by everyone in the community.

 From a very young age, John had a great imagination. He loved to dream and create stories of his own. His favorite place to escape was the library, where he would spend hours immersed in books of all kinds.

 One sunny morning, John stumbled upon an ancient book hidden in the library's dusty shelves. It was titled 'The Mystical World Beyond.' Intrigued by the title, he opened the book and found a handwritten note that said, 'Only a true explorer can unlock the secrets of this book.'

 Excitement rushed through John as he realized this was no ordinary book. With his adventurous spirit, he knew he had to uncover its mysteries.

 He followed the clues in the book, taking him on a thrilling journey through enchanted forests, treacherous mountains, and hidden caves. Along the way, he encountered magical creatures and solved riddles that tested his courage and wisdom.

 As John delved deeper into the enchanted world, he discovered that the book was actually a portal to a magical land. He met fairies, talking animals, and even befriended a wise old wizard who became his guide.

 With every adventure, John learned something new about himself. He discovered bravery he never knew he had and kindness that touched everyone he met. He realized that true strength comes from within, and dreams really do have the power to come true.

 After many thrilling adventures, John finally reached the end of his journey. He stood before a magnificent doorway, the gateway back to his own world. The mystic creatures bid him farewell, and the wise old wizard reminded him, 'No matter where life takes you, John, always remember the magic you carry within your heart.'

 As John closed the book, he felt a sense of accomplishment and gratitude for the incredible journey he had experienced. From that day forward, he continued to write his own stories, sharing his adventures with others and inspiring their hearts.

 And so, the adventures of John Doe became legendary in the village, reminding everyone that with a little bit of magic and a whole lot of imagination, anything is possible.
```