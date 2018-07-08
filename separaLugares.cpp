#include <stdio.h>
#include <string.h>
#include <stdlib.h>

int main(void)
{
  float a, b;
  char s[100];
  FILE *fp = NULL;
  
  while(gets(s) != NULL)
  {
    if(strlen(s) == 0) continue;
    if(strncmp(s, "Lugar", strlen("Lugar")) == 0) {
      if(fp != NULL) fclose(fp);
      if ((fp = fopen(strcat(s, ".txt"), "w")) == NULL) {
        printf("Erro ao abrir o arquivo %s\n", s);
        break;
      }
    }
    else {
      sscanf(s, "%f %f", &a, &b);
      fprintf(fp, "%f %f\n", a, b);
    }
  }
  if(fp != NULL) fclose(fp);
  return 0;
}
