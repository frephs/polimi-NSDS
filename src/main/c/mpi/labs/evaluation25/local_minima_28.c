#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// Group number: 28
// Group members: Federico Grandi, Francesco Genovese, Jonatan Sciaky

const int N = 256;
// const int N = 64;

// Allocates and initializes matrix
int *generate_matrix() {
  int *A = (int *)malloc(N * N * sizeof(int));
  for (int i = 0; i < N * N; i++) {
    A[i] = rand() % 100;
  }
  return A;
}

// Returns the value at the given row and column
int val(int *A, int r, int c) { return A[r * N + c]; }

// ASSUMING we're not in bounds
int isLocalMin(int *rows, int r, int c) {
  int curr = val(rows, r, c);
  int up = val(rows, r - 1, c);
  int down = val(rows, r + 1, c);
  int left = val(rows, r, c - 1);
  int right = val(rows, r, c + 1);

  return curr < up && curr < down && curr < left && curr < right;
}

void print_matrix(int *matrix, int rows, int cols) {
  for (int r = 0; r < rows; r++) {
    for (int c = 0; c < cols; c++) {
      printf("%3d ", val(matrix, r, c));
    }
    printf("\n");
  }
  printf("\n");
  printf("\n");
}

int main(int argc, char **argv) {
  MPI_Init(&argc, &argv);

  int rank, size;
  MPI_Comm_rank(MPI_COMM_WORLD, &rank);
  MPI_Comm_size(MPI_COMM_WORLD, &size);

  // if (N % size != 0) {
  //   if (rank == 0)
  //     printf("N must be a multiple of the number of processes.\n");
  //   MPI_Finalize();
  //   return 0;
  // }

  // ------------------------------------------------------------
  // Generate matrix on rank 0
  // ------------------------------------------------------------

  int *fullA = NULL;
  if (rank == 0) {
    fullA = generate_matrix();
    print_matrix(fullA, N, N);
  }

  // printf("rank: %d, PID: %d\n", rank, getpid());
  // MPI_Barrier(MPI_COMM_WORLD);
  // if (rank == 0) {
  //   printf("Press Enter to continue.\n");
  //   // std::ignore = std::getchar();
  // }
  // MPI_Barrier(MPI_COMM_WORLD);

  // ------------------------------------------------------------
  // Distribute to all processes
  // ------------------------------------------------------------

  int rows_per_proc = N / size;
  
  // Allocate buffer with space for overlap_before + local + overlap_after
  int buffer_rows = rows_per_proc;
  int offset = 0;
  
  if (rank > 0) {
    buffer_rows++;  // Space for overlap_before
    offset = 1;     // Local data starts at row 1
  }
  if (rank < size - 1) {
    buffer_rows++;  // Space for overlap_after
  }
  
  int *buffer = malloc(sizeof(int) * buffer_rows * N);
  int *local = buffer + (offset * N);  // Point to where local data starts

  MPI_Scatter(fullA, N * rows_per_proc, MPI_INT, 
              local, N * rows_per_proc, MPI_INT, 
              0, MPI_COMM_WORLD);

  // ------------------------------------------------------------
  // Free global matrix
  // ------------------------------------------------------------
  if (rank == 0) {
    free(fullA);
  }

  // ------------------------------------------------------------
  // Exchange further information if needed
  // ------------------------------------------------------------

  if (rank > 0) {
    MPI_Send(local, N, MPI_INT, rank - 1, 0, MPI_COMM_WORLD);
    MPI_Recv(buffer, N, MPI_INT, rank - 1, 0, MPI_COMM_WORLD,
             MPI_STATUS_IGNORE);
  }
  if (rank < size - 1) {
    int last_local_row = offset + rows_per_proc - 1;
    MPI_Send(buffer + (last_local_row * N), N, MPI_INT, rank + 1, 0,
             MPI_COMM_WORLD);
    MPI_Recv(buffer + ((last_local_row + 1) * N), N, MPI_INT, rank + 1, 0, 
             MPI_COMM_WORLD, MPI_STATUS_IGNORE);
  }

  // ------------------------------------------------------------
  // Compute local minima (excluding GLOBAL borders)
  // ------------------------------------------------------------

  int *local_results = malloc(sizeof(int) * rows_per_proc);
  for (int i = 0; i < rows_per_proc; i++) {
    local_results[i] = 0;
  }

  // Determine the range of rows to check in the buffer
  int start_row = (rank == 0) ? 1 : offset;
  int end_row = (rank == size - 1) ? offset + rows_per_proc - 1 : offset + rows_per_proc;
  
  for (int r = start_row; r < end_row; r++) {
    for (int c = 1; c < N - 1; c++) {
      if (isLocalMin(buffer, r, c)) {
        local_results[r - offset]++;
      }
    }
  }

  // ------------------------------------------------------------
  // Send results to rank 0 and print results on rank 0
  // ------------------------------------------------------------
  printf("local results: ");
  print_matrix(local_results, 1, rows_per_proc);
  int *results = malloc(sizeof(int) * N);
  MPI_Gather(local_results, rows_per_proc, MPI_INT, results, rows_per_proc,
             MPI_INT, 0, MPI_COMM_WORLD);

  if (rank == 0) {
    for (int r = 0; r < N; r++) {
      printf("- Row: %d, local minima: %d\n", r, results[r]);
    }
  }

  // ------------------------------------------------------------
  // Free allocated memory
  // ------------------------------------------------------------
  free(local_results);
  free(buffer);
  free(results);

  MPI_Finalize();
  return 0;
}