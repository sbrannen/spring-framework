cat ./build/test_classes.txt | grep -v "^#" | grep -v "^$" | sed 's/^/-c=/' | tr '\n' ' '
