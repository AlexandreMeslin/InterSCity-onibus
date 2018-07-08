#include <stdio.h>
int main(void)
{
  float a, b;
  while(scanf("%f%f", &a, &b) != EOF)
  {
    printf("%f %f\n", b, a);
  }
  return 0;
}
