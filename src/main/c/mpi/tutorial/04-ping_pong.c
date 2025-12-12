#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#define MSG_DIMENSION 5

// Simple ping pong program to exemplify MPI_Send and MPI_Recv
// Assume only two processes
int main(int argc, char** argv) {
  const int tot_msgs = 100;

  MPI_Init(NULL, NULL);

  int my_rank;
  MPI_Comm_rank(MPI_COMM_WORLD, &my_rank);

  int num_msgs = 0;

  int other_rank = 1 - my_rank;

  MPI_Status status;

  char* msg = malloc(sizeof(char) * MSG_DIMENSION);
  
  if (my_rank == 0) {
    strcpy(msg, "ping");
    MPI_Send(msg, MSG_DIMENSION, MPI_CHAR, other_rank, 0, MPI_COMM_WORLD);
    num_msgs++;
    
    while (num_msgs < tot_msgs) {
      MPI_Recv(msg, MSG_DIMENSION, MPI_CHAR, other_rank, MPI_ANY_TAG, MPI_COMM_WORLD, &status);
      printf("Rank %d, Message received: %s\n", my_rank, msg);
      num_msgs++;
      
      strcpy(msg, "ping");
      MPI_Send(msg, MSG_DIMENSION, MPI_CHAR, other_rank, 0, MPI_COMM_WORLD);
      num_msgs++;
    }
  }

  if (my_rank == 1) {
    while (num_msgs < tot_msgs) {
      MPI_Recv(msg, MSG_DIMENSION, MPI_CHAR, other_rank, MPI_ANY_TAG, MPI_COMM_WORLD, &status);
      printf("Rank %d, Message received: %s\n", my_rank, msg);
      num_msgs++;
      
      strcpy(msg, "pong");
      MPI_Send(msg, MSG_DIMENSION, MPI_CHAR, other_rank, 0, MPI_COMM_WORLD);
      num_msgs++;
    }
  }

  free(msg); //very important!!!!!!
  MPI_Finalize();
}
