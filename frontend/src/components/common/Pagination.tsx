import React from 'react';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  const getPages = () => {
    if (totalPages <= 5) {
      return Array.from({ length: totalPages }, (_, i) => i + 1);
    }
    if (currentPage <= 3) {
      return [1, 2, 3, 4, '...', totalPages];
    }
    if (currentPage >= totalPages - 2) {
      return [1, '...', totalPages - 3, totalPages - 2, totalPages - 1, totalPages];
    }
    return [1, '...', currentPage - 1, currentPage, currentPage + 1, '...', totalPages];
  };

  return (
    <div className="mt-8 flex flex-wrap items-center justify-center gap-1.5 sm:mt-10 sm:gap-2">
      <button
        onClick={() => onPageChange(Math.max(1, currentPage - 1))}
        disabled={currentPage === 1}
        className="flex h-9 w-9 items-center justify-center rounded-full border border-white/40 bg-white/20 backdrop-blur-md transition-colors hover:bg-white/40 disabled:opacity-50 sm:h-10 sm:w-10"
      >
        &lt;
      </button>

      {getPages().map((page, idx) => (
        <React.Fragment key={idx}>
          {page === '...' ? (
            <span className="flex h-9 w-7 items-center justify-center text-black-blue/50 sm:h-10 sm:w-10">...</span>
          ) : (
            <button
              onClick={() => onPageChange(page as number)}
              className={`flex h-9 w-9 items-center justify-center rounded-full border border-white/40 backdrop-blur-md transition-colors sm:h-10 sm:w-10 ${currentPage === page
                  ? 'bg-white/60 font-bold text-black-blue'
                  : 'bg-white/20 hover:bg-white/40 text-black-blue/70'
                }`}
            >
              {page}
            </button>
          )}
        </React.Fragment>
      ))}

      <button
        onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
        disabled={currentPage === totalPages}
        className="flex h-9 w-9 items-center justify-center rounded-full border border-white/40 bg-white/20 backdrop-blur-md transition-colors hover:bg-white/40 disabled:opacity-50 sm:h-10 sm:w-10"
      >
        &gt;
      </button>
    </div>
  );
}
